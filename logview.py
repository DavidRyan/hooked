#!/usr/bin/env python3
"""
Colorful ADB logcat viewer for Android apps.

Usage:
    python logview.py com.hooked.hooked
    python logview.py com.hooked.hooked --level ERROR
    python logview.py com.hooked.hooked --tag CatchApiService,AuthApiService
    python logview.py com.hooked.hooked --clear
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time
from typing import Optional, Dict, Any, List

# ============================================================================
# ADB Path Detection
# ============================================================================

def find_adb() -> Optional[str]:
    """Find the adb executable."""
    # Check if adb is in PATH
    try:
        result = subprocess.run(['which', 'adb'], capture_output=True, text=True)
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
    except:
        pass
    
    # Common locations
    home = os.path.expanduser('~')
    common_paths = [
        f'{home}/Library/Android/sdk/platform-tools/adb',  # macOS
        f'{home}/Android/Sdk/platform-tools/adb',          # Linux
        '/usr/local/bin/adb',
        '/opt/android-sdk/platform-tools/adb',
    ]
    
    for path in common_paths:
        if os.path.isfile(path) and os.access(path, os.X_OK):
            return path
    
    return None

ADB_PATH = find_adb()

# ============================================================================
# ANSI Color Codes
# ============================================================================

class Colors:
    """ANSI escape codes for terminal colors."""
    RESET = '\033[0m'
    BOLD = '\033[1m'
    DIM = '\033[2m'
    ITALIC = '\033[3m'
    UNDERLINE = '\033[4m'
    
    # Standard colors
    BLACK = '\033[30m'
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    BLUE = '\033[34m'
    MAGENTA = '\033[35m'
    CYAN = '\033[36m'
    WHITE = '\033[37m'
    
    # Bright colors
    BRIGHT_BLACK = '\033[90m'   # Gray
    BRIGHT_RED = '\033[91m'
    BRIGHT_GREEN = '\033[92m'
    BRIGHT_YELLOW = '\033[93m'
    BRIGHT_BLUE = '\033[94m'
    BRIGHT_MAGENTA = '\033[95m'
    BRIGHT_CYAN = '\033[96m'
    BRIGHT_WHITE = '\033[97m'
    
    # Background colors
    BG_RED = '\033[41m'
    BG_GREEN = '\033[42m'
    BG_YELLOW = '\033[43m'
    BG_BLUE = '\033[44m'
    
    @classmethod
    def disable(cls):
        """Disable all colors (for --no-color mode)."""
        for attr in dir(cls):
            if not attr.startswith('_') and attr.isupper():
                setattr(cls, attr, '')


# ============================================================================
# Log Levels
# ============================================================================

class LogLevel:
    """Log level constants and utilities."""
    VERBOSE = 0
    DEBUG = 1
    INFO = 2
    WARNING = 3
    ERROR = 4
    FATAL = 5
    
    _names = {
        'V': VERBOSE, 'VERBOSE': VERBOSE,
        'D': DEBUG, 'DEBUG': DEBUG,
        'I': INFO, 'INFO': INFO,
        'W': WARNING, 'WARN': WARNING, 'WARNING': WARNING,
        'E': ERROR, 'ERROR': ERROR,
        'F': FATAL, 'FATAL': FATAL, 'A': FATAL, 'ASSERT': FATAL,
    }
    
    @classmethod
    def from_string(cls, level_str: str) -> int:
        """Convert level string to int."""
        return cls._names.get(level_str.upper().strip(), cls.DEBUG)
    
    @classmethod
    def colorize(cls, level_str: str) -> str:
        """Return colorized level string."""
        level = level_str.upper().strip()
        
        if level in ('E', 'ERROR'):
            return f"{Colors.BOLD}{Colors.BRIGHT_RED}{level}{Colors.RESET}"
        elif level in ('F', 'FATAL', 'A', 'ASSERT'):
            return f"{Colors.BOLD}{Colors.BG_RED}{Colors.WHITE}{level}{Colors.RESET}"
        elif level in ('W', 'WARN', 'WARNING'):
            return f"{Colors.BOLD}{Colors.YELLOW}{level}{Colors.RESET}"
        elif level in ('I', 'INFO'):
            return f"{Colors.BOLD}{Colors.GREEN}{level}{Colors.RESET}"
        elif level in ('D', 'DEBUG'):
            return f"{Colors.BOLD}{Colors.BLUE}{level}{Colors.RESET}"
        elif level in ('V', 'VERBOSE'):
            return f"{Colors.BOLD}{Colors.WHITE}{level}{Colors.RESET}"
        else:
            return f"{Colors.BOLD}{level}{Colors.RESET}"


# ============================================================================
# Colorization Functions
# ============================================================================

def colorize_timestamp(timestamp: str) -> str:
    """Colorize timestamp."""
    return f"{Colors.CYAN}{timestamp}{Colors.RESET}"


def colorize_tag(tag: str, level: str, max_len: int = 5) -> str:
    """Colorize tag based on log level."""
    display = tag[:max_len].ljust(max_len)
    level = level.upper().strip()
    
    # Color tag based on level for better visibility
    if level in ('E', 'ERROR', 'F', 'FATAL', 'A'):
        return f"{Colors.BRIGHT_RED}{display}{Colors.RESET}"
    elif level in ('W', 'WARN', 'WARNING'):
        return f"{Colors.YELLOW}{display}{Colors.RESET}"
    elif level in ('I', 'INFO'):
        return f"{Colors.GREEN}{display}{Colors.RESET}"
    elif level in ('D', 'DEBUG'):
        return f"{Colors.BLUE}{display}{Colors.RESET}"
    else:
        return f"{Colors.MAGENTA}{display}{Colors.RESET}"


def colorize_http_method(method: str) -> str:
    """Colorize HTTP method."""
    method_upper = method.upper()
    colors = {
        'GET': Colors.BRIGHT_CYAN,
        'POST': Colors.BRIGHT_GREEN,
        'PUT': Colors.BRIGHT_YELLOW,
        'PATCH': Colors.BRIGHT_MAGENTA,
        'DELETE': Colors.BRIGHT_RED,
    }
    color = colors.get(method_upper, Colors.WHITE)
    return f"{Colors.BOLD}{color}{method_upper}{Colors.RESET}"


def colorize_http_status(status_code: int) -> str:
    """Colorize HTTP status code based on category."""
    status_str = str(status_code)
    if 200 <= status_code < 300:
        return f"{Colors.BOLD}{Colors.BRIGHT_GREEN}{status_str}{Colors.RESET}"
    elif 300 <= status_code < 400:
        return f"{Colors.BOLD}{Colors.CYAN}{status_str}{Colors.RESET}"
    elif 400 <= status_code < 500:
        return f"{Colors.BOLD}{Colors.BRIGHT_YELLOW}{status_str}{Colors.RESET}"
    elif 500 <= status_code < 600:
        return f"{Colors.BOLD}{Colors.BRIGHT_RED}{status_str}{Colors.RESET}"
    else:
        return f"{Colors.BOLD}{status_str}{Colors.RESET}"


def colorize_arrow(arrow: str) -> str:
    """Colorize request/response arrows."""
    if arrow == '→':
        return f"{Colors.BOLD}{Colors.BRIGHT_BLUE}→{Colors.RESET}"
    elif arrow == '←':
        return f"{Colors.BOLD}{Colors.BRIGHT_MAGENTA}←{Colors.RESET}"
    return arrow


def colorize_json_value(value: Any, indent: int = 0) -> str:
    """Recursively colorize a JSON value."""
    indent_str = '  ' * indent
    
    if value is None:
        return f"{Colors.BRIGHT_RED}null{Colors.RESET}"
    elif isinstance(value, bool):
        color = Colors.BRIGHT_GREEN if value else Colors.BRIGHT_RED
        return f"{color}{str(value).lower()}{Colors.RESET}"
    elif isinstance(value, (int, float)):
        return f"{Colors.BRIGHT_YELLOW}{value}{Colors.RESET}"
    elif isinstance(value, str):
        escaped = json.dumps(value)
        return f"{Colors.BRIGHT_GREEN}{escaped}{Colors.RESET}"
    elif isinstance(value, list):
        if not value:
            return f"{Colors.WHITE}[]{Colors.RESET}"
        items = []
        for item in value:
            items.append(f"{indent_str}  {colorize_json_value(item, indent + 1)}")
        return f"{Colors.WHITE}[{Colors.RESET}\n" + ",\n".join(items) + f"\n{indent_str}{Colors.WHITE}]{Colors.RESET}"
    elif isinstance(value, dict):
        if not value:
            return f"{Colors.WHITE}{{}}{Colors.RESET}"
        items = []
        for k, v in value.items():
            key_colored = f"{Colors.BRIGHT_CYAN}\"{k}\"{Colors.RESET}"
            val_colored = colorize_json_value(v, indent + 1)
            items.append(f"{indent_str}  {key_colored}: {val_colored}")
        return f"{Colors.WHITE}{{{Colors.RESET}\n" + ",\n".join(items) + f"\n{indent_str}{Colors.WHITE}}}{Colors.RESET}"
    else:
        return str(value)


def pretty_print_json(json_str: str, base_indent: str = "") -> str:
    """Parse and pretty-print JSON with colors."""
    try:
        data = json.loads(json_str)
        lines = colorize_json_value(data).split('\n')
        return '\n'.join(base_indent + line for line in lines)
    except json.JSONDecodeError:
        return f"{base_indent}{Colors.WHITE}{json_str}{Colors.RESET}"


def colorize_message(message: str, level: str) -> tuple:
    """
    Colorize the message content based on log level and content.
    Returns (colorized_main_message, colorized_json_block or None)
    """
    result = message
    json_block = None
    level = level.upper().strip()
    
    # Extract JSON from message if present
    json_match = re.search(r'(\{.+\})\s*$', message, re.DOTALL)
    if json_match:
        json_str = json_match.group(1)
        message_without_json = message[:json_match.start()].rstrip()
        
        try:
            json.loads(json_str)
            result = message_without_json
            json_block = json_str
        except json.JSONDecodeError:
            pass
    
    # Colorize arrows
    result = re.sub(r'→', colorize_arrow('→'), result)
    result = re.sub(r'←', colorize_arrow('←'), result)
    
    # Colorize HTTP methods (bright)
    result = re.sub(
        r'\b(GET|POST|PUT|PATCH|DELETE)\b',
        lambda m: colorize_http_method(m.group(1)),
        result
    )
    
    # Colorize HTTP status codes in brackets [200 OK] or [404 Not Found]
    def colorize_status_bracket(match):
        code = int(match.group(1))
        text = match.group(2)
        colored_code = colorize_http_status(code)
        return f"[{colored_code} {text}]"
    
    result = re.sub(
        r'\[(\d{3})\s+([^\]]+)\]',
        colorize_status_bracket,
        result
    )
    
    # Colorize standalone status codes
    def colorize_status_standalone(match):
        prefix = match.group(1)
        code = int(match.group(2))
        suffix = match.group(3)
        colored_code = colorize_http_status(code)
        return f"{prefix}{colored_code}{suffix}"
    
    result = re.sub(
        r'(← )(\d{3})(\s)',
        colorize_status_standalone,
        result
    )
    
    # Colorize URLs (bright blue, underlined)
    result = re.sub(
        r'(https?://[^\s]+)',
        f"{Colors.UNDERLINE}{Colors.BRIGHT_BLUE}\\1{Colors.RESET}",
        result
    )
    
    # Colorize file paths
    result = re.sub(
        r'(/[a-zA-Z0-9_/\-\.]+\.[a-zA-Z]+)',
        f"{Colors.BRIGHT_WHITE}\\1{Colors.RESET}",
        result
    )
    
    # Colorize class names with line numbers (e.g., "CatchApiService.kt:35")
    result = re.sub(
        r'\b([A-Z][a-zA-Z0-9_]*\.kt):(\d+)\b',
        f"{Colors.BRIGHT_CYAN}\\1{Colors.RESET}:{Colors.BRIGHT_YELLOW}\\2{Colors.RESET}",
        result
    )
    
    # Colorize Java/Kotlin class references (e.g., "com.hooked.CatchApiService")
    result = re.sub(
        r'\b(com\.[a-zA-Z0-9_.]+)\b',
        f"{Colors.CYAN}\\1{Colors.RESET}",
        result
    )
    
    # Colorize exception class names (ending in Exception or Error) - BRIGHT RED
    result = re.sub(
        r'\b([A-Z][a-zA-Z]*(?:Exception|Error|Throwable))\b',
        f"{Colors.BOLD}{Colors.BRIGHT_RED}\\1{Colors.RESET}",
        result
    )
    
    # Colorize "at " stack trace lines
    result = re.sub(
        r'^(\s*at\s+)',
        f"{Colors.BRIGHT_BLACK}\\1{Colors.RESET}",
        result
    )
    
    # Colorize "Caused by:" 
    result = re.sub(
        r'(Caused by:)',
        f"{Colors.BOLD}{Colors.BRIGHT_RED}\\1{Colors.RESET}",
        result
    )
    
    # Colorize keywords based on meaning
    # Error keywords - bright red
    error_keywords = ['error', 'failed', 'failure', 'exception', 'crash', 'fatal', 'null', 'invalid', 'illegal']
    for kw in error_keywords:
        result = re.sub(
            rf'\b({kw})\b',
            f"{Colors.BRIGHT_RED}\\1{Colors.RESET}",
            result,
            flags=re.IGNORECASE
        )
    
    # Success keywords - bright green
    success_keywords = ['success', 'successful', 'ok', 'created', 'deleted', 'loaded', 'complete', 'done', 'ready', 'connected']
    for kw in success_keywords:
        result = re.sub(
            rf'\b({kw})\b',
            f"{Colors.BRIGHT_GREEN}\\1{Colors.RESET}",
            result,
            flags=re.IGNORECASE
        )
    
    # Warning keywords - bright yellow
    warning_keywords = ['warning', 'warn', 'retry', 'retrying', 'slow', 'deprecated', 'timeout', 'unavailable']
    for kw in warning_keywords:
        result = re.sub(
            rf'\b({kw})\b',
            f"{Colors.BRIGHT_YELLOW}\\1{Colors.RESET}",
            result,
            flags=re.IGNORECASE
        )
    
    # Info keywords - bright cyan
    info_keywords = ['loading', 'fetching', 'starting', 'initializing', 'processing', 'saving', 'reading', 'writing']
    for kw in info_keywords:
        result = re.sub(
            rf'\b({kw})\b',
            f"{Colors.BRIGHT_CYAN}\\1{Colors.RESET}",
            result,
            flags=re.IGNORECASE
        )
    
    # Colorize UUIDs - magenta
    result = re.sub(
        r'\b([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})\b',
        f"{Colors.BRIGHT_MAGENTA}\\1{Colors.RESET}",
        result,
        flags=re.IGNORECASE
    )
    
    # Colorize numbers with units - yellow
    result = re.sub(
        r'\b(\d+)\s*(ms|MB|KB|GB|bytes|items|catches|records|rows|s|seconds|minutes|min)\b',
        f"{Colors.BRIGHT_YELLOW}\\1{Colors.RESET} \\2",
        result,
        flags=re.IGNORECASE
    )
    
    # Colorize standalone numbers - yellow
    result = re.sub(
        r'\b(\d+)\b',
        f"{Colors.YELLOW}\\1{Colors.RESET}",
        result
    )
    
    # Colorize memory addresses and hex values - dim magenta
    result = re.sub(
        r'\b(0x[a-fA-F0-9]+)\b',
        f"{Colors.MAGENTA}\\1{Colors.RESET}",
        result
    )
    
    # Colorize quoted strings - green
    result = re.sub(
        r'"([^"]*)"',
        f'{Colors.BRIGHT_GREEN}"\\1"{Colors.RESET}',
        result
    )
    
    # Colorize single-quoted strings - green
    result = re.sub(
        r"'([^']*)'",
        f"{Colors.BRIGHT_GREEN}'\\1'{Colors.RESET}",
        result
    )
    
    # Colorize boolean values
    result = re.sub(
        r'\b(true)\b',
        f"{Colors.BRIGHT_GREEN}\\1{Colors.RESET}",
        result,
        flags=re.IGNORECASE
    )
    result = re.sub(
        r'\b(false)\b',
        f"{Colors.BRIGHT_RED}\\1{Colors.RESET}",
        result,
        flags=re.IGNORECASE
    )
    
    # Apply overall color tint based on log level
    if level in ('E', 'ERROR', 'F', 'FATAL', 'A'):
        # Keep error message vibrant - don't dim it
        pass
    elif level in ('W', 'WARNING'):
        pass
    elif level in ('I', 'INFO'):
        pass
    elif level in ('D', 'DEBUG'):
        pass
    
    return result, json_block


# ============================================================================
# Log Parsing
# ============================================================================

def parse_logcat_line(line: str) -> Optional[Dict[str, str]]:
    """
    Parse a standard logcat line.
    
    Format 1 (time): "MM-DD HH:MM:SS.mmm LEVEL/TAG(PID): message"
    Example: "01-19 17:54:16.950 D/AsyncImage(6192): Loading image..."
    
    Format 2 (threadtime): "MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: message"
    Example: "01-19 17:54:16.950  6192  6192 D AsyncImage: Loading image..."
    """
    # Try format 1: "MM-DD HH:MM:SS.mmm LEVEL/TAG(PID): message"
    match = re.match(
        r'(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+'  # timestamp
        r'([VDIWEFA])/([^\(]+)\(\s*(\d+)\):\s*'         # level/tag(pid):
        r'(.*)',                                         # message
        line
    )
    
    if match:
        return {
            'timestamp': match.group(1),
            'level': match.group(2),
            'tag': match.group(3).strip(),
            'pid': match.group(4),
            'tid': match.group(4),  # Same as PID for this format
            'message': match.group(5),
            'raw': line,
        }
    
    # Try format 2: "MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: message"
    match = re.match(
        r'(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+'  # timestamp
        r'(\d+)\s+(\d+)\s+'                              # PID TID
        r'([VDIWEFA])\s+'                                # level
        r'([^:]+):\s*'                                   # tag
        r'(.*)',                                         # message
        line
    )
    
    if match:
        return {
            'timestamp': match.group(1),
            'pid': match.group(2),
            'tid': match.group(3),
            'level': match.group(4),
            'tag': match.group(5).strip(),
            'message': match.group(6),
            'raw': line,
        }
    
    return None


def is_stack_trace_line(message: str) -> bool:
    """Check if a message looks like part of a stack trace."""
    msg = message.strip()
    if msg.startswith('at '):
        return True
    if msg.startswith('Caused by:'):
        return True
    if re.match(r'^\.{3} \d+ more$', msg):  # "... 10 more"
        return True
    # Exception class with message: "java.lang.Exception: some message"
    if re.match(r'^[a-zA-Z_][a-zA-Z0-9_.]*(?:Exception|Error|Throwable):', msg):
        return True
    return False


def format_log_line(parsed: Dict[str, str], show_json: bool = True) -> str:
    """Format a parsed log line with full colorization."""
    level = parsed['level']
    msg = parsed['message']
    
    timestamp = colorize_timestamp(parsed['timestamp'])
    level_colored = LogLevel.colorize(level)
    message, json_block = colorize_message(msg, level)
    
    # For stack trace continuation lines, use minimal formatting (no tag)
    if is_stack_trace_line(msg):
        indent = "    "  # 4 spaces
        main_line = f"{level_colored} {indent}{message}"
    else:
        tag = colorize_tag(parsed['tag'], level)
        main_line = f"{level_colored} {tag} {message}"
    
    # Add JSON block if present
    if json_block and show_json:
        indent = " " * 50
        json_formatted = pretty_print_json(json_block, indent)
        return f"{main_line}\n{json_formatted}"
    
    return main_line


def format_separator(level: str) -> str:
    """Return a separator line based on level."""
    width = 60
    level = level.upper().strip()
    
    if level in ('E', 'ERROR', 'F', 'FATAL', 'A'):
        return f"{Colors.BRIGHT_RED}{'━' * width}{Colors.RESET}"
    elif level in ('W', 'WARNING'):
        return f"{Colors.YELLOW}{'─' * width}{Colors.RESET}"
    return f"{Colors.BRIGHT_BLACK}{'─' * width}{Colors.RESET}"


# ============================================================================
# Banner & UI
# ============================================================================

def print_banner(package: str, level: str, tags: Optional[str]):
    """Print a startup banner."""
    fish = "🎣"
    
    # Build info lines
    info_lines = [
        f"  Package: {Colors.BRIGHT_CYAN}{package}{Colors.RESET}",
        f"  Level:   {Colors.BRIGHT_GREEN}{level}+{Colors.RESET}",
    ]
    if tags:
        info_lines.append(f"  Tags:    {Colors.BRIGHT_YELLOW}{tags}{Colors.RESET}")
    info_lines.append(f"  {Colors.BRIGHT_BLACK}Press Ctrl+C to exit{Colors.RESET}")
    
    # Calculate box width
    max_len = max(len(re.sub(r'\033\[[0-9;]*m', '', line)) for line in info_lines)
    box_width = max(max_len + 4, 40)
    
    # Print box
    print()
    print(f"  {Colors.BRIGHT_CYAN}╭{'─' * box_width}╮{Colors.RESET}")
    print(f"  {Colors.BRIGHT_CYAN}│{Colors.RESET}  {fish} {Colors.BOLD}{Colors.BRIGHT_CYAN}Hooked LogView{Colors.RESET}" + " " * (box_width - 18) + f"{Colors.BRIGHT_CYAN}│{Colors.RESET}")
    print(f"  {Colors.BRIGHT_CYAN}├{'─' * box_width}┤{Colors.RESET}")
    for line in info_lines:
        visible_len = len(re.sub(r'\033\[[0-9;]*m', '', line))
        padding = box_width - visible_len - 1
        print(f"  {Colors.BRIGHT_CYAN}│{Colors.RESET}{line}{' ' * padding}{Colors.BRIGHT_CYAN}│{Colors.RESET}")
    print(f"  {Colors.BRIGHT_CYAN}╰{'─' * box_width}╯{Colors.RESET}")
    print()


def print_reconnect_message(package: str):
    """Print a message when reconnecting."""
    print()
    print(f"  {Colors.BRIGHT_YELLOW}⟳ App restarted, reconnecting to {package}...{Colors.RESET}")
    print()


def print_waiting_message(package: str):
    """Print a message when waiting for app to start."""
    print(f"  {Colors.BRIGHT_BLACK}Waiting for {package} to start...{Colors.RESET}", end='\r')


def print_exit_message():
    """Print exit message."""
    print()
    print(f"  {Colors.BRIGHT_BLACK}Goodbye! 👋{Colors.RESET}")
    print()


# ============================================================================
# ADB Functions
# ============================================================================

def run_adb(args: List[str], **kwargs) -> subprocess.CompletedProcess:
    """Run an ADB command."""
    if not ADB_PATH:
        raise FileNotFoundError("adb not found")
    return subprocess.run([ADB_PATH] + args, **kwargs)


def run_adb_popen(args: List[str], **kwargs) -> subprocess.Popen:
    """Run an ADB command with Popen."""
    if not ADB_PATH:
        raise FileNotFoundError("adb not found")
    return subprocess.Popen([ADB_PATH] + args, **kwargs)


def get_pid(package: str) -> Optional[str]:
    """Get the PID for a package, or None if not running."""
    try:
        result = run_adb(
            ['shell', 'pidof', '-s', package],
            capture_output=True,
            text=True,
            timeout=5
        )
        pid = result.stdout.strip()
        return pid if pid else None
    except (subprocess.TimeoutExpired, subprocess.SubprocessError):
        return None


def clear_logcat():
    """Clear the logcat buffer."""
    try:
        run_adb(['logcat', '-c'], timeout=5)
    except (subprocess.TimeoutExpired, subprocess.SubprocessError):
        pass


def check_adb() -> bool:
    """Check if ADB is available and a device is connected."""
    if not ADB_PATH:
        return False
    try:
        result = run_adb(
            ['devices'],
            capture_output=True,
            text=True,
            timeout=5
        )
        lines = result.stdout.strip().split('\n')
        devices = [l for l in lines[1:] if l.strip() and 'device' in l]
        return len(devices) > 0
    except (subprocess.TimeoutExpired, subprocess.SubprocessError, FileNotFoundError):
        return False


# ============================================================================
# Main Logic
# ============================================================================

def passes_filters(parsed: Dict[str, str], min_level: int, tag_filter: Optional[List[str]]) -> bool:
    """Check if a parsed log line passes the filters."""
    # Level filter
    log_level = LogLevel.from_string(parsed['level'])
    if log_level < min_level:
        return False
    
    # Tag filter
    if tag_filter:
        tag_lower = parsed['tag'].lower()
        if not any(t.lower() in tag_lower for t in tag_filter):
            return False
    
    return True


def stream_logs(package: str, min_level: int, tag_filter: Optional[List[str]], show_json: bool):
    """Stream and display logs, reconnecting if the app restarts."""
    last_level = None
    in_error_block = False
    
    while True:
        # Get PID
        pid = get_pid(package)
        
        if not pid:
            print_waiting_message(package)
            time.sleep(1)
            continue
        
        # Clear waiting message
        print(" " * 60, end='\r')
        
        # Start logcat
        process = run_adb_popen(
            ['logcat', '-v', 'time', f'--pid={pid}'],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        try:
            stdout = process.stdout
            if stdout is None:
                time.sleep(1)
                continue
                
            for line in stdout:
                # Check if process has ended (app crashed/stopped)
                if process.poll() is not None:
                    break
                
                line = line.rstrip()
                if not line:
                    continue
                
                # Parse the line
                parsed = parse_logcat_line(line)
                if not parsed:
                    # Print unparseable lines (like stack traces) with color based on last level
                    if last_level in ('E', 'F', 'A'):
                        # Colorize stack trace elements
                        colored_line = line
                        # Highlight "at " prefix
                        colored_line = re.sub(r'^(\s*at\s+)', f'{Colors.BRIGHT_BLACK}\\1{Colors.RESET}', colored_line)
                        # Highlight class.method
                        colored_line = re.sub(r'([a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z_][a-zA-Z0-9_]*)\(', f'{Colors.BRIGHT_RED}\\1{Colors.RESET}(', colored_line)
                        # Highlight file:line
                        colored_line = re.sub(r'\(([^:]+):(\d+)\)', f'({Colors.CYAN}\\1{Colors.RESET}:{Colors.BRIGHT_YELLOW}\\2{Colors.RESET})', colored_line)
                        # Highlight Caused by:
                        colored_line = re.sub(r'(Caused by:)', f'{Colors.BOLD}{Colors.BRIGHT_RED}\\1{Colors.RESET}', colored_line)
                        # Highlight exception names
                        colored_line = re.sub(r'\b([A-Z][a-zA-Z]*(?:Exception|Error))\b', f'{Colors.BOLD}{Colors.BRIGHT_RED}\\1{Colors.RESET}', colored_line)
                        print(colored_line)
                    elif last_level == 'W':
                        print(f"{Colors.YELLOW}{line}{Colors.RESET}")
                    else:
                        print(f"{Colors.BRIGHT_BLACK}{line}{Colors.RESET}")
                    continue
                
                # Apply filters
                if not passes_filters(parsed, min_level, tag_filter):
                    continue
                
                current_level = parsed['level'].upper()
                is_error = current_level in ('E', 'F', 'A')
                
                # Close error block before printing non-error line
                if in_error_block and not is_error:
                    print(format_separator('E'))
                    in_error_block = False
                
                # Open error block before first error line
                if is_error and not in_error_block:
                    print(format_separator(current_level))
                    in_error_block = True
                
                # Print the formatted line
                print(format_log_line(parsed, show_json))
                sys.stdout.flush()
                
                last_level = current_level
                
        except Exception as e:
            pass
        finally:
            process.terminate()
            try:
                process.wait(timeout=2)
            except subprocess.TimeoutExpired:
                process.kill()
        
        # App probably restarted, try to reconnect
        print_reconnect_message(package)
        time.sleep(1)


def main():
    parser = argparse.ArgumentParser(
        description='Colorful ADB logcat viewer for Android apps',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s com.hooked.hooked
  %(prog)s com.hooked.hooked --level ERROR
  %(prog)s com.hooked.hooked --tag CatchApiService,AuthApiService
  %(prog)s com.hooked.hooked --level WARNING --tag Api --clear
        """
    )
    
    parser.add_argument(
        'package',
        help='Android package name (e.g., com.hooked.hooked)'
    )
    parser.add_argument(
        '--level',
        default='VERBOSE',
        help='Minimum log level: VERBOSE, DEBUG, INFO, WARNING, ERROR (default: VERBOSE)'
    )
    parser.add_argument(
        '--tag',
        help='Filter by tag(s), comma-separated (e.g., CatchApiService,AuthApiService)'
    )
    parser.add_argument(
        '--clear',
        action='store_true',
        help='Clear logcat buffer before starting'
    )
    parser.add_argument(
        '--no-json',
        action='store_true',
        help='Disable JSON pretty-printing'
    )
    parser.add_argument(
        '--no-color',
        action='store_true',
        help='Disable colors (for piping to files)'
    )
    
    args = parser.parse_args()
    
    # Disable colors if requested
    if args.no_color:
        Colors.disable()
    
    # Check ADB
    if not ADB_PATH:
        print(f"{Colors.BRIGHT_RED}Error: adb not found.{Colors.RESET}")
        print(f"{Colors.BRIGHT_BLACK}Searched in PATH and common locations.{Colors.RESET}")
        print(f"{Colors.BRIGHT_BLACK}Install Android SDK or add adb to your PATH.{Colors.RESET}")
        sys.exit(1)
    
    if not check_adb():
        print(f"{Colors.BRIGHT_RED}Error: No Android device connected.{Colors.RESET}")
        print(f"{Colors.BRIGHT_BLACK}Connect a device or start an emulator.{Colors.RESET}")
        sys.exit(1)
    
    # Parse filters
    min_level = LogLevel.from_string(args.level)
    tag_filter = [t.strip() for t in args.tag.split(',')] if args.tag else None
    show_json = not args.no_json
    
    # Clear logcat if requested
    if args.clear:
        clear_logcat()
    
    # Print banner
    print_banner(args.package, args.level.upper(), args.tag)
    
    # Stream logs
    try:
        stream_logs(args.package, min_level, tag_filter, show_json)
    except KeyboardInterrupt:
        print_exit_message()
        sys.exit(0)


if __name__ == '__main__':
    main()
