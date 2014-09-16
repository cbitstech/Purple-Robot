from django.conf.urls import patterns, include, url
from django.contrib.sitemaps import *
from django.views.generic import RedirectView
from django.views.decorators.cache import cache_page

from views import *

urlpatterns = patterns('',
#    url(r'^(?P<config>.+).scm$', pr_configuration, name='pr_configuration'),
    url(r'^print$', ingest_payload_print, name='ingest_payload_print'),
    url(r'^log$', log_event, name='log_event'),
    url(r'^$', ingest_payload, name='ingest_payload'),
)
