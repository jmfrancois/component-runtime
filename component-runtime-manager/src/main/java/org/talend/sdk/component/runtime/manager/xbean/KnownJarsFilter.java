/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.xbean;

import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;

public class KnownJarsFilter implements Predicate<String> {

    public static final Predicate<String> INSTANCE = new KnownJarsFilter();

    @Getter
    private final Collection<String> excludes = new HashSet<String>() {

        {
            add("accessors-smart");
            add("activation-");
            add("activeio-");
            add("activemq-");
            add("aeron");
            add("aether-");
            add("agrona");
            add("akka-");
            add("animal-sniffer-annotation");
            add("annotation");
            add("ant-");
            add("antlr-");
            add("antlr4-");
            add("aopalliance-");
            add("apache-mime4j");
            add("apacheds-");
            add("apache-el");
            add("ApacheJMeter");
            add("api-asn1-");
            add("api-common-");
            add("api-util-");
            add("apiguardian-api-");
            add("app-");
            add("archaius-core");
            add("args4j-");
            add("arquillian-");
            add("asciidoctorj-");
            add("asm-");
            add("aspectj");
            add("async-http-client-");
            add("autoschema-");
            add("auto-value-");
            add("avalon-framework-");
            add("avro-");
            add("avro4s-");
            add("awaitility-");
            add("aws-");
            add("axis-");
            add("axis2-");
            add("base64-");
            add("batchee-jbatch");
            add("batik-");
            add("bcmail");
            add("bcpkix");
            add("bcprov-");
            add("beam-model-");
            add("beam-runners-");
            add("beam-sdks-");
            add("bigtable-client-");
            add("bigtable-protos-");
            add("boilerpipe-");
            add("bonecp");
            add("bootstrap.jar");
            add("brave-");
            add("bsf-");
            add("build-link");
            add("bval");
            add("byte-buddy");
            add("c3p0-");
            add("cache");
            add("carrier");
            add("cassandra-driver-core");
            add("catalina-");
            add("catalina.jar");
            add("cats");
            add("cdi-");
            add("cglib-");
            add("charsets.jar");
            add("chill");
            add("classindex");
            add("classmate");
            add("classutil");
            add("classycle");
            add("cldrdata");
            add("commands-");
            add("common-");
            add("commons-");
            add("component-api");
            add("component-form");
            add("component-runtime");
            add("components-adapter-beam");
            add("components-api");
            add("components-common");
            add("component-server");
            add("component-spi");
            add("component-studio");
            add("compress-lzf");
            add("config");
            add("constructr");
            add("container-core");
            add("contenttype");
            add("coverage-agent");
            add("cryptacular-");
            add("cssparser-");
            add("curator-");
            add("curvesapi-");
            add("cxf-");
            add("daikon");
            add("databinding");
            add("dataquality");
            add("dataset-");
            add("datastore-");
            add("debugger-agent");
            add("deltaspike-");
            add("deploy.jar");
            add("derby-");
            add("derbyclient-");
            add("derbynet-");
            add("dnsns");
            add("dom4j");
            add("draw2d");
            add("easymock-");
            add("ecj-");
            add("eclipselink-");
            add("ehcache-");
            add("el-api");
            add("enumeratum");
            add("enunciate-core-annotations");
            add("error_prone_annotations");
            add("expressions");
            add("FastInfoset");
            add("fastutil");
            add("feign-core");
            add("feign-hystrix");
            add("feign-slf4j");
            add("filters-helpers");
            add("findbugs-");
            add("fluent-hc");
            add("fluentlenium-core");
            add("fontbox");
            add("freemarker-");
            add("fusemq-leveldb-");
            add("gax-");
            add("gcsio-");
            add("gef-");
            add("geocoder");
            add("geronimo-");
            add("gmbal");
            add("google-");
            add("gpars-");
            add("gragent.jar");
            add("graph");
            add("grizzled-scala");
            add("grizzly-");
            add("groovy-");
            add("grpc-");
            add("gson-");
            add("guava-");
            add("guice-");
            add("h2-");
            add("hadoop-");
            add("hamcrest-");
            add("hawtbuf-");
            add("hawtdispatch-");
            add("hawtio-");
            add("hawtjni-runtime");
            add("HdrHistogram");
            add("help-");
            add("hibernate-");
            add("HikariCP");
            add("hk2-");
            add("howl-");
            add("hsqldb-");
            add("htmlunit-");
            add("htrace-");
            add("httpclient-");
            add("httpcore-");
            add("httpmime");
            add("hystrix");
            add("iban4j-");
            add("icu4j-");
            add("idb-");
            add("idea_rt.jar");
            add("instrumentation-api");
            add("ion-java");
            add("isoparser-");
            add("istack-commons-runtime-");
            add("ivy-");
            add("j2objc-annotations");
            add("jaccess");
            add("jackcess-");
            add("jackson-");
            add("janino-");
            add("jansi-");
            add("jasper-el.jar");
            add("jasper.jar");
            add("jasypt-");
            add("java-atk-wrapper");
            add("javacsv");
            add("javaee-");
            add("javaee-api");
            add("javassist-");
            add("java-libpst-");
            add("java-support-");
            add("javaws.jar");
            add("javax.");
            add("java-xmlbuilder-");
            add("jaxb-");
            add("jaxp-");
            add("jbake-");
            add("jBCrypt");
            add("jboss-");
            add("jbossall-");
            add("jbosscx-");
            add("jbossjts-");
            add("jbosssx-");
            add("jcache");
            add("jce.jar");
            add("jcip-annotations");
            add("jcl-over-slf4j-");
            add("jcommander-");
            add("jdbcdslog-1");
            add("jempbox");
            add("jersey-");
            add("jets3t");
            add("jettison-");
            add("jetty-");
            add("jface");
            add("jfairy");
            add("jffi");
            add("jfr.jar");
            add("jfxrt.jar");
            add("jfxswt");
            add("jhighlight");
            add("jjwt");
            add("jline");
            add("jmatio-");
            add("jmdns-");
            add("jmespath-");
            add("jms");
            add("jmustache");
            add("jna-");
            add("jnr-");
            add("jobs-");
            add("joda-convert");
            add("joda-time-");
            add("johnzon-");
            add("jolokia-");
            add("joni-");
            add("jopt-simple");
            add("jruby-");
            add("json4s-");
            add("jsonb-api");
            add("json-");
            add("jsoup-");
            add("jsp-api");
            add("jsr");
            add("jsse.jar");
            add("jta");
            add("juli-");
            add("jul-to-slf4j-");
            add("junrar-");
            add("junit-");
            add("juniversalchardet");
            add("junit5-");
            add("jwt");
            add("jython");
            add("kafka");
            add("kahadb-");
            add("kotlin-runtime");
            add("kryo");
            add("leveldb");
            add("libphonenumber");
            add("lift-json");
            add("lmdbjava");
            add("localedata");
            add("log4j-");
            add("logback");
            add("logging-event-layout");
            add("logkit-");
            add("lombok");
            add("lucene");
            add("lz4");
            add("machinist");
            add("macro-compat");
            add("mail-");
            add("management-");
            add("mapstruct-");
            add("maven-");
            add("mbean-annotation-api-");
            add("meecrowave-");
            add("mesos-");
            add("metadata-extractor-");
            add("metrics-");
            add("microprofile-config-api");
            add("microprofile-openapi-api");
            add("microprofile-opentracing-api");
            add("mimepull-");
            add("mina-");
            add("minlog");
            add("mockito-core");
            add("mqtt-client-");
            add("multitenant-core");
            add("multiverse-core-");
            add("mx4j-");
            add("myfaces-");
            add("mysql-connector-java-");
            add("nashorn");
            add("neethi-");
            add("nekohtml-");
            add("neko-htmlunit");
            add("netflix");
            add("netty-");
            add("nimbus-jose-jwt");
            add("objenesis-");
            add("okhttp");
            add("okio");
            add("opentracing-api");
            add("opencensus-");
            add("openejb-");
            add("openjpa-");
            add("openmdx-");
            add("opennlp-");
            add("opensaml-");
            add("opentest4j-");
            add("openwebbeans-");
            add("openws-");
            add("options");
            add("ops4j-");
            add("org.apache.aries");
            add("org.apache.commons");
            add("org.apache.log4j");
            add("org.eclipse.");
            add("org.junit.");
            add("org.osgi.annotation.versioning");
            add("org.osgi.core-");
            add("org.osgi.enterprise");
            add("org.talend");
            add("orient-commons-");
            add("orientdb-core-");
            add("orientdb-nativeos-");
            add("oro-");
            add("osgi");
            add("paranamer");
            add("parquet");
            add("pax-url");
            add("pdfbox");
            add("PDFBox");
            add("play");
            add("plexus-");
            add("plugin.jar");
            add("poi-");
            add("postgresql");
            add("preferences-");
            add("prefixmapper");
            add("proto-");
            add("protobuf-");
            add("py4j-");
            add("pyrolite-");
            add("qdox-");
            add("quartz-2");
            add("quartz-openejb-");
            add("reactive-streams");
            add("reflections");
            add("reflectasm-");
            add("regexp-");
            add("registry-");
            add("resources.jar");
            add("rhino");
            add("ribbon");
            add("rmock-");
            add("RoaringBitmap-");
            add("rome");
            add("routes-compiler");
            add("routines");
            add("rt.jar");
            add("runners");
            add("runtime-");
            add("rxjava");
            add("rxnetty");
            add("saaj-");
            add("sac-");
            add("scala");
            add("scalap");
            add("scalatest");
            add("scannotation-");
            add("selenium");
            add("serializer-");
            add("serp-");
            add("service-common");
            add("servlet-api-");
            add("servo-");
            add("shaded");
            add("shapeless");
            add("shrinkwrap-");
            add("sisu-guice");
            add("sisu-inject");
            add("slf4j-");
            add("slick");
            add("smack-");
            add("smackx-");
            add("snakeyaml-");
            add("snappy-");
            add("spark-");
            add("specs2");
            add("spring-");
            add("sshd-");
            add("ssl-config-core");
            add("stax2-api-");
            add("stax-api-");
            add("stream");
            add("sunec.jar");
            add("sunjce_provider");
            add("sunpkcs11");
            add("surefire-");
            add("swagger-");
            add("swizzle-");
            add("sxc-");
            add("system-rules");
            add("tachyon-");
            add("tagsoup-");
            add("talend-icon");
            add("test-agent");
            add("test-interface");
            add("testng-");
            add("threetenbp");
            add("tika-");
            add("tomcat");
            add("tomee-");
            add("tools.jar");
            add("twirl");
            add("twitter4j-");
            add("tyrex");
            add("uncommons");
            add("unused");
            add("util");
            add("validation-api-");
            add("velocity-");
            add("wandou");
            add("wagon-");
            add("webbeans-");
            add("websocket");
            add("woodstox-core");
            add("workbench");
            add("ws-commons-util-");
            add("wsdl4j-");
            add("wss4j-");
            add("wstx-asl-");
            add("xalan-");
            add("xbean-");
            add("xercesImpl-");
            add("xlsx-streamer-");
            add("xml-apis-");
            add("xmlbeans-");
            add("xmlenc-");
            add("xmlgraphics-");
            add("xmlpcore");
            add("xmlpull-");
            add("xml-resolver-");
            add("xmlrpc-");
            add("xmlschema-");
            add("XmlSchema-");
            add("xmlsec-");
            add("xmltooling-");
            add("xmlunit-");
            add("xstream-");
            add("xz-");
            add("zipfs.jar");
            add("zipkin-");
            add("ziplock-");
            add("zkclient");
            add("zookeeper-");
            add("zstd-");
            ofNullable(System.getProperty("talend.component.runtime.manager.exclusions"))
                    .map(s -> s.split(","))
                    .ifPresent(e -> Stream.of(e).map(String::trim).filter(v -> !v.isEmpty()).forEach(this::add));
        }
    };

    @Override
    public boolean test(final String jarName) {
        return excludes.stream().noneMatch(jarName::startsWith);
    }
}
