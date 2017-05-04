import static ch.qos.logback.classic.Level.*
import static ch.qos.logback.core.spi.FilterReply.DENY
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL
import ch.qos.logback.classic.boolex.GEventEvaluator
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.filter.EvaluatorFilter

def patternExpression = "%date{HH:mm:ss.SSS} \\(%F:%L\\) [%logger{0}] [%level] - %msg%n%ex{short}"
def BASEDIR = System.getProperty("basedir", ".")

appender("STDERR", ConsoleAppender) {
    // log to STDERR only messages at the ERROR level
    filter(EvaluatorFilter) {
      evaluator(GEventEvaluator) {
        expression = 'e.level.toInt() >= WARN.toInt()'
      }
      onMatch = NEUTRAL
      onMismatch = DENY
    }
    encoder(PatternLayoutEncoder) {
      pattern = patternExpression
    }
    target = "System.err"
  }

appender("FILE", FileAppender) {
    file = "${BASEDIR}/log/usermanager.log"
    append = true
    encoder(PatternLayoutEncoder) {
        pattern = patternExpression
    }
}

logger("edu.illinois.i3.htrc.usermanager", DEBUG)
logger("org.apache.axis2", OFF)
logger("org.apache.axiom.util.stax.dialect.StAXDialectDetector", ERROR)

root(INFO, ["STDERR","FILE"])