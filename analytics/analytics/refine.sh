cat $1 | \
                    sed  's/^\([0-9][0-9]*\)\(.*History FINE\)\(.*\) / history \1 \3 /' | \
                    sed 's/\(^[0-9][0-9]*\)\(.*ParamsStatus FINE\)\(.*\)/status \1 \3/' | \
                    sed 's/^.*BrigandGen.*$//' | \
                    grep -v '^$' > $1_refined.txt
