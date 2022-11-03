package io.slingr.svcs.services.logs;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

/**
 * <p>PatternLayout to use to format Log4j messages. The following parameters where added to the default log4j
 * character converters: <ul>
 *     <li><b>a</b>: App name</li>
 *     <li><b>e</b>: Service name</li>
 *     <li><b>v</b>: Environment name</li>
 *     <li><b>y</b>: Deployment name</li>
 * </ul>
 *
 * @see org.apache.log4j.Layout
 * @see org.apache.log4j.PatternLayout
 *
 * <p>Created by lefunes on 25/09/15.
 */
public class SvcLayout extends PatternLayout {
    private static final char APP_NAME_CHAR = 'a';
    private static final char POD_ID_CHAR = 'i';
    private static final char SVC_NAME_CHAR = 'e';
    private static final char ENVIRONMENT_NAME_CHAR = 'v';
    private static final char DEPLOYMENT_NAME_CHAR = 'y';
    private static final char COMPONENT_NAME_CHAR = 'c';
    private static final char LINE_BREAK_CHAR = 'j';

    private static String application = "-";
    private static String podId = "-";
    private static String svc = "-";
    private static String environment = "-";
    private static String deployment = "-";
    private static String component = "service";

    public static void setApplication(String application) {
        SvcLayout.application = application;
    }

    public static void setPodId(String podId) {
        SvcLayout.podId = podId;
    }

    public SvcLayout() {
    }

    public SvcLayout(String pattern) {
        super(pattern);
    }

    public static void setSvc(String svc) {
        SvcLayout.svc = svc;
    }

    public static void setEnvironment(String environment) {
        SvcLayout.environment = environment;
    }

    public static void setDeployment(String deployment) {
        SvcLayout.deployment = deployment;
    }

    public static void setComponent(String component) {
        SvcLayout.component = component;
    }

    @Override
    protected PatternParser createPatternParser(String pattern) {
        return new SvcPatternParser(pattern);
    }

    private class SvcPatternParser extends PatternParser{

        public SvcPatternParser(String pattern) {
            super(pattern);
        }

        @Override
        protected void finalizeConverter(char c) {
            switch (c) {
                case APP_NAME_CHAR:
                    currentLiteral.setLength(0);
                    addConverter(new AppNameConverter());
                    break;
                case POD_ID_CHAR:
                    currentLiteral.setLength(0);
                    addConverter(new PodIdConverter());
                    break;
                case SVC_NAME_CHAR:
                    currentLiteral.setLength(0);
                    addConverter(new SvcNameConverter());
                    break;
                case ENVIRONMENT_NAME_CHAR:
                    currentLiteral.setLength(0);
                    addConverter(new EnvironmentNameConverter());
                    break;
                case DEPLOYMENT_NAME_CHAR:
                    currentLiteral.setLength(0);
                    addConverter(new DeploymentNameConverter());
                    break;
                case COMPONENT_NAME_CHAR:
                    currentLiteral.setLength(0);
                    addConverter(new ComponentConverter());
                    break;
                case LINE_BREAK_CHAR:
                    currentLiteral.setLength(0);
                    addConverter(new BreakLineConverter());
                    break;
                default:
                    super.finalizeConverter(c);
            }
        }
    }

    private class AppNameConverter extends PatternConverter {
        @Override
        protected String convert(LoggingEvent evt) {
            return application;
        }
    }

    private class PodIdConverter extends PatternConverter {
        @Override
        protected String convert(LoggingEvent evt) {
            String podSuffix = podId;
            if (podSuffix.length() > 5) {
                podSuffix = podSuffix.substring(podSuffix.length()-5);
            }
            return podSuffix;
        }
    }

    private class SvcNameConverter extends PatternConverter {
        @Override
        protected String convert(LoggingEvent evt) {
            return svc;
        }
    }

    private class EnvironmentNameConverter extends PatternConverter {
        @Override
        protected String convert(LoggingEvent evt) {
            return environment;
        }
    }

    private class DeploymentNameConverter extends PatternConverter {
        @Override
        protected String convert(LoggingEvent evt) {
            return deployment;
        }
    }

    private class ComponentConverter extends PatternConverter {
        @Override
        protected String convert(LoggingEvent evt) {
            return component;
        }
    }

    private class BreakLineConverter extends PatternConverter {

        @Override
        protected String convert(LoggingEvent evt) {
            // it is an exception
            if (evt.getThrowableInformation() != null) {
                return ": " + StringUtils.join(evt.getThrowableInformation().getThrowableStrRep(), "\n");
            } else {
                return "\n";
            }
        }
    }
}
