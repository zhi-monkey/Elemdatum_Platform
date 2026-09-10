#!/bin/bash

eval "$(cat | tr -d '\r' | grep -E '^[^#;]' | xargs -d'\n' -n1 | sed 's/^/export /')"