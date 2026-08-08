import subprocess
import sys
import os
import platform
import time

def free_port(port=8080):
    if platform.system() != "Windows":
        return
    
    print(f"Checking if port {port} is in use...")
    try:
        # Run netstat to find process using the port
        output = subprocess.check_output("netstat -ano", shell=True).decode("utf-8", errors="ignore")
        
        pids_to_kill = set()
        for line in output.splitlines():
            if f":{port}" in line and "LISTENING" in line:
                parts = line.strip().split()
                if len(parts) >= 5:
                    pid = parts[-1]
                    try:
                        pids_to_kill.add(int(pid))
                    except ValueError:
                        pass
        
        # Kill the processes
        my_pid = os.getpid()
        for pid in pids_to_kill:
            if pid == my_pid:
                continue
            print(f"Port {port} is held by PID {pid}. Killing it...")
            subprocess.run(f"taskkill /F /PID {pid}", shell=True)
            time.sleep(1) # Give it a second to release the port
    except Exception as e:
        print(f"Error freeing port {port}: {e}")

def load_env():
    if os.path.exists(".env"):
        with open(".env") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#"):
                    if "=" in line:
                        key, val = line.split("=", 1)
                        os.environ[key.strip()] = val.strip(' "\'')

if __name__ == "__main__":
    load_env()
    free_port(8080)
    print("Starting Spring Boot Backend Service on port 8080...")
    subprocess.run("mvn spring-boot:run", shell=True)
