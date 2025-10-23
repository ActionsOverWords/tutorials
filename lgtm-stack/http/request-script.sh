TIMES=5
for i in $(eval echo "{1..$TIMES}")
do
    siege -c 3 -r 5 http://localhost:8080/random_sleep
    siege -c 3 -r 5 http://localhost:8080/cpu_task
    siege -c 3 -r 5 http://localhost:8080/random_status
    siege -c 3 -r 5 http://localhost:8080/chain
    siege -c 1 -r 10 http://localhost:8080/error_test
    sleep 5
done
