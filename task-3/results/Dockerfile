FROM postgres:17
COPY --chmod=755 export.sh /usr/local/bin/export-shipments
ENTRYPOINT ["/usr/local/bin/export-shipments"]
