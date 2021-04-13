package org.openvoipalliance.voiplib.model

class PathConfigurations(private val basePath: String) {
    val linphoneConfigFile = "$basePath/.linphonerc"
    val linphoneFactoryConfigFile = "$basePath/linphonerc"
}