%JAVA_HOME%\bin\java %JAVA_OPTS% -ea -XX:+UseSerialGC ^
    -Dfile.encoding=UTF-8 ^
    -Djava.util.logging.config.file=logging.properties ^
    -p %USERPROFILE%\.m2\repository\com\oracle\ojdbc10\19.3\ojdbc10-19.3.jar;%USERPROFILE%\.m2\repository\com\ibm\db2\jcc\11.1.4.4\jcc-11.1.4.4.jar;target\classes ^
    -m de.steg0.deskapps.tabletool/de.steg0.deskapps.tabletool.Myriad ^
    -config test.myriad.properties ^
    test.myr.xml
