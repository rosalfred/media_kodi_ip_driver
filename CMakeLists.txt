cmake_minimum_required(VERSION 2.8.3)

project(smarthome_media_kodi_driver)
set(PACKAGE_PATH "org/rosmultimedia/player")

find_package(catkin REQUIRED
  rosjava_build_tools

  smarthome_common_driver
  smarthome_media_model
)

catkin_rosjava_setup(publishMavenJavaPublicationToMavenRepository installApp)

catkin_package()

install(DIRECTORY ${CATKIN_DEVEL_PREFIX}/${CATKIN_GLOBAL_MAVEN_DESTINATION}/${PACKAGE_PATH}/${PROJECT_NAME}/
    DESTINATION ${CATKIN_GLOBAL_MAVEN_DESTINATION}/${PACKAGE_PATH}/${PROJECT_NAME})

install(DIRECTORY build/install/${PROJECT_NAME}/lib/
    DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}/build/lib)

install(PROGRAMS build/install/${PROJECT_NAME}/bin/${PROJECT_NAME}
    DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}/build/scripts)

install(DIRECTORY launch/
    DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}/launch)
