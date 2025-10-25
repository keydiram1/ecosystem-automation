#!/bin/bash

content+="  http:\n"
content+="    rate:\n"
content+="      size: 1\n"
content+="      tps: 1\n"

echo -e "$content" >> conf/service/config.yml

