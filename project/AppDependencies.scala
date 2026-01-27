import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.5.0"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"   % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"   % "12.29.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "4.3.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"       % bootstrapVersion
  ).map(_ % Test)

  val it = Seq.empty
}
