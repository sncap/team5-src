
#!/bin/bash
docker rmi -f docker images | grep "swa" | awk '{print $3}'
