#!/bin/sh

PROFILES_ACTIVE=$LOG_MODE
NACOS_ADDR=$NACOS_URL
REAL_APP_ID=$APP_ID

if [ "$REAL_APP_ID" = "" ]; then
   REAL_APP_ID="nacosGateway"
fi

if [ "$LOG_MODE" = "" ]; then
   PROFILES_ACTIVE="prod"
fi

if [ "$NACOS_ADDR" = "" ]; then
   NACOS_ADDR="nacos-0.nacos-headless.default.svc.cluster.local"
fi

cd /server
java -jar -server ./nacosgateway.jar \
    -Xms2048M -Xmx2048M -XX:PermSize=256m -XX:MaxPermSize=512m \
    --apollo.meta=$APOLLO_META \
    --spring.profiles.active=$PROFILES_ACTIVE \
    --app.id=$REAL_APP_ID \
    --spring.cloud.nacos.discovery.server-addr=$NACOS_ADDR
