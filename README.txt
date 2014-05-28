Hi.
At first, my english is not good.

This sample project is intended to try With clause(Common Table Expression) in Slick.

Originally, I wanted to search hierarchical data. so I tried Slick's StaticQuery but It was not useful in this case.
So I tried this.

In order to test this sample project,
you need unofficial Slick version(2.1.0-SNAPSHOT, Scala2.10) and postgresql 9.x.
Please check this: https://github.com/imcharsi/slick/tree/20140524

This branch is based on master(https://github.com/slick/slick) and has
tminglei(https://github.com/tminglei/slick)'s window function patch
(https://github.com/imcharsi/slick/commit/83a4a31870fd7440bb85ef268431faa0e641d9cc)

and

JurajBurian(https://github.com/JurajBurian/slick)'s intersect-diff patch
(https://github.com/imcharsi/slick/commit/62c455640ec332ca98cf5c64c618067e60773d34).

And some codes are copied from https://github.com/tminglei/slick-pg to use array.

Additionally, There is FunctionTable feature.
I need this as workaround for variable parameters problem of the Where clause's IN operator.

In this sample project, There are 3 example queries and these can be used with Slick's Compiled.

Follow things are result sql. Notice that there are no indentations in original result sql.

=== Fibonacci
select x2.x3
from (
    with recursive "fib" ("fiba", "fibb", "seed", "num") as (
        select 0, 0, 1, 1
        from "generate_series"(1, 1) x4
        union
        select x5."seed" + x5."fiba", x5."fiba" + x5."fibb", x5."fiba", x5."num" + 1
        from "fib" x5
        where x5."num" < 12
    )
    select x6."fiba" as x3 from "fib" x6
) x2
===

=== Factorial
select x2.x3, x2.x4
from (
    with recursive "facto" ("fact", "num") as (
        select 1, 1
        from "generate_series"(1, 1) x5
        union
        select x6."fact" * (x6."num" + 1), x6."num" + 1
        from "facto" x6
        where x6."num" < 6
    )
    select x7."fact" as x3, x7."num" as x4
    from "facto" x7
) x2
===

=== Hierachical
select x2.x3, x2.x4, x2.x5, x2.x6, x2.x7
from (
    with recursive "with_two" ("unnest") as (
        select unnest(cast(string_to_array('1,11,31',',') as int4 ARRAY))
        from "generate_series"(1, 1) x8
    ),
    "with_three" ("id", "parent_id", "name") as (
        select *
        from (
            with recursive "with_three" ("id", "parent_id", "name") as (
                select x9."id", x9."parent_id", x9."name"
                from "sample"."model_a" x9
            )
            select x10."id", x10."parent_id", x10."name"
            from "with_three" x10
        ) x11
    ),
    "with_one" ("id", "parent_id", "name", "odr", "depth") as (
        select
            x12."id", x12."parent_id", x12."name",
            conv_array(cast(row_number() over( partition by x12."parent_id" order by x12."name" desc) as INTEGER)),
            0
        from "sample"."model_a" x12
        where (x12."parent_id" is null) and (x12."name" like '%')
        union
        select x13.x14, x13.x15, x13.x16, x13.x17, x13.x18
        from (
            select
                x19.x20 as x14, x19.x21 as x15, x19.x22 as x16,
                x23.x24 || cast(row_number() over( partition by x19.x21 order by x19.x22 desc) as INTEGER) as x17,
                cast((x23.x25 + 1) as INTEGER) as x18
            from (
                select x26."odr" as x24, x26."depth" as x25, x26."id" as x27
                from "with_one" x26
            ) x23
            inner join (
                select x28."id" as x20, x28."parent_id" as x21, x28."name" as x22
                from "sample"."model_a" x28
            ) x19
            on x23.x27 = x19.x21
        ) x13
        where (x13.x15 in (select x29."unnest" from "with_two" x29)) and (x13.x16 like '%')
    )
    select x30."odr" as x6, x30."depth" as x7, x30."id" as x3, x30."name" as x5, x30."parent_id" as x4
    from "with_one" x30
    order by x30."odr"
) x2
===

and user-defined sql function is
===
-- Function: conv_array(integer)

-- DROP FUNCTION conv_array(integer);

CREATE OR REPLACE FUNCTION conv_array(i integer)
  RETURNS integer[] AS
$BODY$
select array[i]
$BODY$
  LANGUAGE sql IMMUTABLE
  COST 100;
ALTER FUNCTION conv_array(integer)
  OWNER TO sample_admin;
===

Thanks.
