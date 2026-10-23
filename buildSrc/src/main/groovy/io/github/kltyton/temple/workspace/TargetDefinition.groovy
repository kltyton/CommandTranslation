package io.github.kltyton.temple.workspace

final class TargetDefinition {
    final File directory
    final String id
    final String loader
    final String minecraft
    final int javaVersion
    final int compilerJavaVersion
    final int runtimeJavaVersion
    final int gradleJavaVersion
    final Properties properties

    TargetDefinition(File directory, Properties properties) {
        this.directory = directory.canonicalFile
        this.id = directory.name
        this.properties = properties
        this.loader = properties.getProperty('loader')
        this.minecraft = properties.getProperty('minecraft_version')
        this.javaVersion = Integer.parseInt(properties.getProperty('java_version'))
        this.compilerJavaVersion = Integer.parseInt(properties.getProperty('compiler_java_version', javaVersion.toString()))
        this.runtimeJavaVersion = Integer.parseInt(properties.getProperty('runtime_java_version', javaVersion.toString()))
        this.gradleJavaVersion = Integer.parseInt(properties.getProperty('gradle_java_version'))
    }

    String taskSuffix() { id.replace('-', '_').replace('.', '_') }

    List<Integer> toolchainVersions() {
        ([gradleJavaVersion, compilerJavaVersion, runtimeJavaVersion] +
                properties.getProperty('tool_java_versions', '').tokenize(',').collect { Integer.parseInt(it.trim()) }).unique()
    }

    Map<String, Object> describe() {
        [target: id, loader: loader, minecraft: minecraft, java: javaVersion,
         compiler_java: compilerJavaVersion, runtime_java: runtimeJavaVersion, gradle_java: gradleJavaVersion,
         java_toolchains: toolchainVersions().join('\n'), ci: properties.getProperty('ci_enabled', 'true') == 'true']
    }
}
