Before you run the collector, you need to provide the URL for Elastic APM and the Secret Token

Where to get the info?
1. Login to Elastic Cloud
2. Navigate to Fleet > Agent policies > Elastic Cloud agent policy 
3. Click on Elastic APM
4. Find Elastic APM URL under General section
5. Find Secret Token under Agent authorization

To run:
docker-compose up -d

To stop:
docker-compose down -v

