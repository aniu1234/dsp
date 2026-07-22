#!/usr/bin/env bash

set -u

BASE_HOME="$(cd "$(dirname "$0")" && pwd)"
APP_MAINCLASS="com.qinyadan.system.dsp.FrontEndMain"
PID_FILE="${BASE_HOME}/../dsp.pid"
LOG_DIR="${BASE_HOME}/../logs"
LOG_FILE="${LOG_DIR}/dsp.log"
CLASSPATH="${BASE_HOME}/../config:${BASE_HOME}/../lib/*${CLASSPATH:+:${CLASSPATH}}"
JAVA_OPTS_VALUE="${JAVA_OPTS:--Xms512m -Xmx512m -Xmn256m -Djava.awt.headless=true}"

if [[ -n "${JAVA_HOME:-}" ]]; then
    JAVA_BIN="${JAVA_HOME}/bin/java"
else
    JAVA_BIN="$(command -v java || true)"
fi

if [[ -z "${JAVA_BIN}" || ! -x "${JAVA_BIN}" ]]; then
    echo "Java executable not found. Set JAVA_HOME or add java to PATH."
    exit 1
fi

psid=0

checkpid() {
    psid=0
    if [[ -f "${PID_FILE}" ]]; then
        local candidate
        candidate="$(tr -d '[:space:]' < "${PID_FILE}")"
        if [[ "${candidate}" =~ ^[0-9]+$ ]] && kill -0 "${candidate}" 2>/dev/null; then
            psid="${candidate}"
        else
            rm -f "${PID_FILE}"
        fi
    fi
}

start() {
    checkpid
    if [[ "${psid}" -ne 0 ]]; then
        echo "${APP_MAINCLASS} is already running (pid=${psid})"
        return 0
    fi

    mkdir -p "${LOG_DIR}"
    echo "Starting ${APP_MAINCLASS} ..."
    # JAVA_OPTS is intentionally expanded so callers can supply multiple JVM arguments.
    nohup "${JAVA_BIN}" ${JAVA_OPTS_VALUE} -classpath "${CLASSPATH}" "${APP_MAINCLASS}" \
        >> "${LOG_FILE}" 2>&1 &
    psid=$!
    echo "${psid}" > "${PID_FILE}"
    sleep 1
    checkpid

    if [[ "${psid}" -ne 0 ]]; then
        echo "(pid=${psid}) [OK]"
    else
        echo "[Failed]. See ${LOG_FILE}"
        return 1
    fi
}

stop() {
    checkpid
    if [[ "${psid}" -eq 0 ]]; then
        echo "${APP_MAINCLASS} is not running"
        return 0
    fi

    local target_pid="${psid}"
    echo "Stopping ${APP_MAINCLASS} (pid=${target_pid}) ..."
    kill "${target_pid}"
    for _ in {1..30}; do
        if ! kill -0 "${target_pid}" 2>/dev/null; then
            rm -f "${PID_FILE}"
            echo "[OK]"
            return 0
        fi
        sleep 1
    done

    echo "Graceful shutdown timed out; forcing process termination."
    kill -9 "${target_pid}"
    rm -f "${PID_FILE}"
}

status() {
    checkpid
    if [[ "${psid}" -ne 0 ]]; then
        echo "${APP_MAINCLASS} is running (pid=${psid})"
    else
        echo "${APP_MAINCLASS} is not running"
    fi
}

info() {
    echo "JAVA_BIN=${JAVA_BIN}"
    "${JAVA_BIN}" -version
    echo "BASE_HOME=${BASE_HOME}"
    echo "APP_MAINCLASS=${APP_MAINCLASS}"
    echo "LOG_FILE=${LOG_FILE}"
}

case "${1:-}" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    status)
        status
        ;;
    info)
        info
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|info}"
        exit 1
        ;;
esac
