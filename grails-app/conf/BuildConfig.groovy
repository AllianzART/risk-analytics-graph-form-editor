import org.apache.ivy.plugins.resolver.FileSystemResolver

grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn"

    repositories {
        grailsHome()
        grailsCentral()
    }

    def ulcClientJarResolver = new FileSystemResolver()
    String absolutePluginDir = grailsSettings.projectPluginsDir.absolutePath

    ulcClientJarResolver.addArtifactPattern "${absolutePluginDir}/ulc-[revision]/web-app/lib/[artifact].[ext]"
    ulcClientJarResolver.addArtifactPattern "${basedir}/web-app/lib/[artifact]-[revision].[ext]"
    ulcClientJarResolver.name = "ulc"

    resolver ulcClientJarResolver

    mavenRepo "https://build.intuitive-collaboration.com/maven/plugins/"

    String ulcVersion = "ria-suite-u2"

    plugins {
        runtime ":background-thread:1.3"
        runtime ":hibernate:1.3.7"
        runtime ":joda-time:0.5"
        runtime ":maven-publisher:0.7.5"
        runtime ":quartz:0.4.2"
        runtime ":spring-security-core:1.1.2"
        runtime ":jetty:1.2-SNAPSHOT"
        compile "com.canoo:ulc:${ulcVersion}"
        runtime "org.pillarone:pillar-one-ulc-extensions:0.1"

        test ":code-coverage:1.2.2"

        if (appName == "RiskAnalyticsGraphFormEditor") {
            runtime "org.pillarone:risk-analytics-core:1.4-ALPHA-2.5.1-kti"
            runtime("org.pillarone:risk-analytics-graph-core:0.3.1") { transitive = false }
        }

    }

    dependencies {
        compile group: 'canoo', name: 'ulc-applet-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-base-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-base-trusted', version: ulcVersion
        compile group: 'canoo', name: 'ulc-jnlp-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-servlet-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-standalone-client', version: ulcVersion

        compile group: 'canoo', name: 'ULCGraph-client', version: "0.1.5"
        compile group: 'canoo', name: 'ULCGraph-shared', version: "0.1.5"
        compile group: 'jgraphx', name: 'jgraphx', version: "1.5.1.11"
    }
}

grails.project.dependency.distribution = {
    String passPhrase = ""
    String scpUrl = ""
    try {
        Properties properties = new Properties()
        properties.load(new File("${userHome}/deployInfo.properties").newInputStream())

        passPhrase = properties.get("passPhrase")
        scpUrl = properties.get("url")
    } catch (Throwable t) {
    }
    remoteRepository(id: "pillarone", url: scpUrl) {
        authentication username: 'root', privateKey: "${userHome.absolutePath}/.ssh/id_rsa", passphrase: passPhrase
    }
}

coverage {
    exclusions = [
            'models/**',
            '**/*Test*',
            '**/com/energizedwork/grails/plugins/jodatime/**',
            '**/grails/util/**',
            '**/org/codehaus/**',
            '**/org/grails/**',
            '**GrailsPlugin**',
            '**TagLib**'
    ]

}
