#!/bin/bash
# =====================================================================
#  Toktobaev_Nooruzbek_system_report.sh
#  Author : Nooruzbek Toktobaev
#  Purpose: system monitoring report: student name, date/time, user,
#           OS + kernel, CPU, memory, disk usage and available space.
#           Output is printed to the terminal AND appended to the
#           personal log file (tee -a), so every run adds a new entry.
#  Usage  : ./scripts/Toktobaev_Nooruzbek_system_report.sh
#  Works on Linux; has fallbacks for macOS (sw_vers, sysctl, vm_stat).
# =====================================================================

STUDENT_NAME="Nooruzbek Toktobaev"

# Project root = parent of the scripts/ folder, so the script works from any directory
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_FILE="$PROJECT_DIR/logs/Toktobaev_Nooruzbek_system_report.log"

mkdir -p "$(dirname "$LOG_FILE")"

# Prints a section header
section() {
    echo
    echo "---- $1 ----"
}

report() {
    echo "======================================================="
    echo " SYSTEM REPORT: $STUDENT_NAME"
    echo "======================================================="
    echo "Student name : $STUDENT_NAME"
    echo "Date / time  : $(date '+%Y-%m-%d %H:%M:%S %Z')"
    echo "Username     : $(whoami)"
    echo "Hostname     : $(hostname)"

    section "Operating system and kernel"
    if [ -r /etc/os-release ]; then
        # shellcheck disable=SC1091
        . /etc/os-release
        echo "OS           : $PRETTY_NAME"
    elif command -v sw_vers >/dev/null 2>&1; then
        echo "OS           : $(sw_vers -productName) $(sw_vers -productVersion)"
    fi
    echo "Kernel       : $(uname -sr)"
    echo "uname -a     : $(uname -a)"

    section "CPU"
    if command -v lscpu >/dev/null 2>&1; then
        lscpu | grep -E '^(Architecture|Model name|CPU\(s\)|Thread\(s\) per core|Core\(s\) per socket)'
    else
        echo "Model name   : $(sysctl -n machdep.cpu.brand_string 2>/dev/null)"
        echo "CPU(s)       : $(sysctl -n hw.ncpu 2>/dev/null)"
    fi
    echo "nproc        : $(nproc 2>/dev/null || sysctl -n hw.ncpu)"
    echo "Load average : $(uptime | awk -F'load average[s]*: ' '{print $2}')"

    section "Memory"
    if command -v free >/dev/null 2>&1; then
        free -h
    else
        vm_stat | head -6
    fi

    section "Disk usage (df -h)"
    # Hide virtual filesystems on Linux; macOS df has no -x, so fall back to plain df -h
    df -h -x tmpfs -x devtmpfs -x overlay -x squashfs 2>/dev/null || df -h

    section "Available disk space"
    echo "Root (/)        : $(df -h / | awk 'NR==2 {print $4 " available of " $2 " (" $5 " used)"}')"
    echo "Project dir     : $(df -h "$PROJECT_DIR" | awk 'NR==2 {print $4 " available"}')"
    echo "Project size    : $(du -sh "$PROJECT_DIR" 2>/dev/null | cut -f1) ($PROJECT_DIR)"
    echo
}

report 2>&1 | tee -a "$LOG_FILE"
echo "Report by $STUDENT_NAME appended to $LOG_FILE"
