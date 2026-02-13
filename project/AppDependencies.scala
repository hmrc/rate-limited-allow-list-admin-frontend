import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.5.0"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"   % "12.29.0",

    /* Warning: Version 4.1.0 made changes that mean that a controller cannot be tested.
     * The body parser can either be fixed return a fixed result - either no content to be parsed or the content to
     * be hardcoded which will not allow for the testing of a variety of scenarios.
     */
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "4.0.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"       % bootstrapVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"              % "3.2.17.0"
  ).map(_ % Test)

  val it = Seq.empty
}
