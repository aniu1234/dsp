select a.smallint_type, b.int_type
from csv.t1 a
         inner join test.t1 b on a.int_type = b.int_type
