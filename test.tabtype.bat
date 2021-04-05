%JAVA_HOME%\bin\java %JAVA_OPTS% -ea -XX:+UseSerialGC ^
    -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9001 ^
    -Dfile.encoding=UTF-8 ^
    -Djava.util.logging.config.file=test.logging.properties ^
    -p %USERPROFILE%\.m2\repository\com\oracle\ojdbc10\19.3\ojdbc10-19.3.jar;%USERPROFILE%\.m2\repository\com\ibm\db2\jcc\11.1.4.4\jcc-11.1.4.4.jar;target\classes ^
    -m de.steg0.deskapps.tabletool/de.steg0.deskapps.tabletool.Tabtype ^
    -config test.tabtype.properties ^
    test.tt.xml
