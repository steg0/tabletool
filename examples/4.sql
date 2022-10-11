select * from t
fetch first 1 rows only
;

begin
	update t set s4=3 where n=1;
end
;
--CSV Result
--N,C,S4,B
--5,oracle.sql.CLOB@1ad22024,7,oracle.sql.BLOB@509a16d5

