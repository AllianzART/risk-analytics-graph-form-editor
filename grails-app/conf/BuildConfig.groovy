grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn"

    repositories {
        grailsHome()
        grailsCentral()
    }

    //even though this plugin does not need anything from this repo, it has to be added for the deploy script to check existing plugins
    mavenRepo "https://build.intuitive-collaboration.com/maven/plugins/"

    String ulcVersion = "2008-u4-4.1"

    plugins {
        runtime ":background-thread:1.3"
        runtime ":hibernate:1.3.4"
        runtime ":joda-time:0.5"
        runtime ":maven-publisher:0.7.5"
        runtime ":quartz:0.4.1"
        runtime ":spring-security-core:1.0.1"
        runtime ":jetty:1.2-SNAPSHOT"
        compile "com.canoo:ulc:${ulcVersion}"

        test ":code-coverage:1.1.7"

        runtime "org.pillarone:risk-analytics-core:1.3-ALPHA-3.1.1"
        runtime("org.pillarone:risk-analytics-graph-core:0.1") { transitive = false }
    }

    dependencies {
        compile group: 'canoo', name: 'ulc-applet-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-base-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-base-trusted', version: ulcVersion
        compile group: 'canoo', name: 'ulc-ejb-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-jnlp-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-servlet-client', version: ulcVersion
        compile group: 'canoo', name: 'ulc-standalone-client', version: ulcVersion
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
