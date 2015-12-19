lazy val root = (project in file(".")).
  settings(
    name := "hello",
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(    
        "com.typesafe.akka" %% "akka-actor"                           % "2.4.1",
        "com.typesafe.akka" %% "akka-http-core-experimental"          % "2.0-M2",
        "com.typesafe.akka" %% "akka-http-experimental"               % "2.0-M2",
        "com.typesafe.akka" %% "akka-persistence"                     % "2.4.1",
        "org.iq80.leveldb"  % "leveldb"                               % "0.7",
        "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % "2.0-M2"
    )
  )
