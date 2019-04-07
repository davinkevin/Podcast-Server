FROM nginx:latest
# Author: DAVIN Kevin davin.kevin@gmail.com

COPY default.conf /etc/nginx/conf.d/default.conf
COPY podcast /var/www/podcast

RUN chmod +x /var/www/podcast/create-podcast.bash && /var/www/podcast/create-podcast.bash
