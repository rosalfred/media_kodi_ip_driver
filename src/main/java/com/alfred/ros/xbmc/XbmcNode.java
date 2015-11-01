/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.alfred.ros.xbmc;

import media_msgs.MediaAction;
import media_msgs.MediaGetItem;
import media_msgs.MediaGetItemRequest;
import media_msgs.MediaGetItemResponse;
import media_msgs.MediaGetItems;
import media_msgs.MediaGetItemsRequest;
import media_msgs.MediaGetItemsResponse;
import media_msgs.StateData;
import media_msgs.ToggleMuteSpeaker;
import media_msgs.ToggleMuteSpeakerRequest;
import media_msgs.ToggleMuteSpeakerResponse;

import org.ros.exception.ServiceException;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.service.ServiceResponseBuilder;
import org.xbmc.android.jsonrpc.api.call.JSONRPC.Ping;
import org.xbmc.android.jsonrpc.api.call.JSONRPC.Version;

import com.alfred.ros.core.BaseNodeMain;
import com.alfred.ros.media.MediaMessageConverter;
import com.alfred.ros.media.MediaStateDataComparator;
import com.alfred.ros.xbmc.internal.XbmcLibrary;
import com.alfred.ros.xbmc.internal.XbmcMonitor;
import com.alfred.ros.xbmc.internal.XbmcPlayer;
import com.alfred.ros.xbmc.internal.XbmcSpeaker;
import com.alfred.ros.xbmc.internal.XbmcSystem;
import com.alfred.ros.xbmc.jsonrpc.XbmcJson;

/**
 * Xbmc ROS Node.
 *
 * @author Erwan Le Huitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class XbmcNode extends BaseNodeMain<XbmcConfig, StateData, MediaAction> implements IXbmcNode {

    public static final String SRV_MUTE_SPEAKER_TOGGLE = "speaker_mute_toggle";
    public static final String SRV_MEDIA_GET_ITEM = "get_item";
    public static final String SRV_MEDIA_GET_ITEMS = "get_items";

    private XbmcJson xbmcJson;

    private XbmcLibrary library;
    private XbmcSpeaker speaker;

    public XbmcNode() {
        super("xbmc",
                new MediaStateDataComparator(),
                new MediaMessageConverter(),
                MediaAction._TYPE,
                StateData._TYPE);
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        super.onStart(connectedNode);
        this.startFinal();
    }

    @Override
    public void onShutdown(Node node) {
        super.onShutdown(node);
    }

    @Override
    protected void onConnected() {
        this.getStateData().setState(StateData.ENABLE);
    }

    @Override
    protected void onDisconnected() {
        this.getStateData().setState(StateData.UNKNOWN);
    }

    @Override
    public void onNewMessage(MediaAction message) {
        if (message != null) {
            this.logI(String.format("Command \"%s\"... for %s",
                    message.getMethod(),
                    message.getUri()));

            super.onNewMessage(message);
        }
    }

    @Override
    protected void initialize() {
        super.initialize();

        String url = String.format("http://%s:%d/jsonrpc",
                this.configuration.getHost(),
                this.configuration.getPort());

        this.xbmcJson = new XbmcJson(
                url, this.configuration.getUser(), this.configuration.getPassword());

        this.library = new XbmcLibrary(this.xbmcJson, this);
        this.speaker = new XbmcSpeaker(this.xbmcJson, this);

        this.addModule(new XbmcMonitor());
        this.addModule(new XbmcPlayer(this.xbmcJson, this));
        this.addModule(new XbmcSystem(this.xbmcJson, this));
        this.addModule(this.speaker);
    }

    @Override
    protected void initServices() {
        super.initServices();

        this.getConnectedNode().newServiceServer(
                this.configuration.getPrefix() + SRV_MUTE_SPEAKER_TOGGLE,
                ToggleMuteSpeaker._TYPE,
                new ServiceResponseBuilder<ToggleMuteSpeakerRequest, ToggleMuteSpeakerResponse>() {
                    @Override
                    public void build(ToggleMuteSpeakerRequest request,
                            ToggleMuteSpeakerResponse response) throws ServiceException {
                        XbmcNode.this.speaker.handleSpeakerMuteToggle(request, response);
                    }
                });

        this.getConnectedNode().newServiceServer(
                this.configuration.getPrefix() + SRV_MEDIA_GET_ITEM,
                MediaGetItem._TYPE,
                new ServiceResponseBuilder<MediaGetItemRequest, MediaGetItemResponse>() {
                    @Override
                    public void build(MediaGetItemRequest request,
                            MediaGetItemResponse response) throws ServiceException {
                        XbmcNode.this.library.handleMediaGetItem(request, response);
                    }
                });

        this.getConnectedNode().newServiceServer(
                this.configuration.getPrefix() + SRV_MEDIA_GET_ITEMS,
                MediaGetItems._TYPE,
                new ServiceResponseBuilder<MediaGetItemsRequest, MediaGetItemsResponse>() {
                    @Override
                    public void build(MediaGetItemsRequest request,
                            MediaGetItemsResponse response) throws ServiceException {
                        XbmcNode.this.library.handleMediaGetItems(request, response);
                    }
                });
    }

    @Override
    protected boolean connect() {
        boolean result = false;

        this.logI(String.format("Connecting to %s:%s...",
                this.configuration.getHost(),
                this.configuration.getPort()));

        if (this.pingXbmc()) {
            this.getStateData().setState(StateData.INIT);

            result = true;
            Version.VersionResult version = this.xbmcJson.getResult(new Version());

            if (version.major >= 6) {
                this.logI(String.format(
                        "\tDetected XBMC JSON-RPC version : %d.%d.%d . Done",
                        version.major,
                        version.minor,
                        version.patch));
            } else {
                this.logI(String.format(
                        "\tDetected XBMC JSON-RPC version : %d.%d.%d . Upgrade your XBMC ! this drivers is only available for >= 6.x.x JSON-RPC",
                        version.major,
                        version.minor,
                        version.patch));
            }

            this.logI("\tConnected done.");
        } else {
            this.getStateData().setState(StateData.SHUTDOWN);

            try {
                Thread.sleep(10000 / this.configuration.getRate());
            } catch (InterruptedException e) {
                this.logE(e);
            }
        }

        return result;
    }

    @Override
    protected XbmcConfig getConfig() {
        return new XbmcConfig(this.getConnectedNode());
    }

    private boolean pingXbmc() {
        String ping = this.xbmcJson.getResult(new Ping());
        return ping != null && ping.equals("pong");
    }
}
