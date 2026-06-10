FROM joseluisq/static-web-server:2.43.0

COPY podcast-server /podcast-server
COPY sws.toml /

# V3 is the default UI served at the root URL — V1 stays in the image as a
# fallback under /podcast-server/v1/. Overriding SERVER_ROOT and
# SERVER_FALLBACK_PAGE in Kubernetes flips the active version without
# rebuilding the image. Both env vars are honoured because sws.toml
# intentionally omits `root` and `page-fallback` (config-file values take
# precedence over env in SWS — see ui/src/docker/sws.toml).
ENV SERVER_ROOT=/podcast-server/v3
ENV SERVER_FALLBACK_PAGE=/podcast-server/v3/index.html
