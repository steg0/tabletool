%JAVA_HOME%\bin\java %JAVA_OPTS% -XX:+UseSerialGC ^
    -Dfile.encoding=UTF-8 ^
    -Djava.util.logging.config.file=logging.properties ^
    -p %USERPROFILE%\.m2\repository\com\oracle\ojdbc7\12.1.0.1\ojdbc7-12.1.0.1.jar;%USERPROFILE%\.m2\repository\com\oracle\ojdbc10\19.3\ojdbc10-19.3.jar;p:\jdbc_sqlj\db2jcc4.jar;target\classes ^
    -m de.steg0.deskapps.tabletool/de.steg0.deskapps.tabletool.Myriad ^
    -config test.myriad.properties ^
    test.myr.xml
