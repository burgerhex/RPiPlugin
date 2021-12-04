Members: Aviv Shai

GitHub repo: https://github.com/burgerhex/RPiPlugin

Demo Video: https://youtu.be/M38-bs-gTqE

External libraries used:
- Bukkit, for developing the Minecraft plugin (https://getbukkit.org/download/spigot)
- SmoothieCharts, for real-time in-browser graphing (http://smoothiecharts.org/)
- GrovePi, of course, for interacting with the sensor nodes on the RPi (https://www.dexterindustries.com/grovepi/)

Instructions on how to use:
- Create a new project on Google Cloud Platform
- Make a VM for that project with at least 4 GB of memory
- Add firewall rules to allow incoming traffic from ports 25565, 57944, and 57945
- SSH into the VM, upload the server.zip file on Vocareum onto the VM, and unzip it
    - This already contains a pre-compiled version of the plugin
    - To compile it yourself, open the repo as a Maven project and build it
    - Move the newly created jar to the plugins folder in the server folder on the VM
- Start the server by running /start.bat in the server folder and wait for it to fully start
- SSH into the RPi and clone the GitHub repo
- Attach the GrovePi sensors as implied by the variables at the top of src/client/main.py
- Join the Minecraft server by running Minecraft and joining the VM's external IP, shown in GCP
    - If you didn't spawn in the big obsidian box, let yourself inside
- Run src/client/main.py
    - You should now be able to control the walls, fire, and platforms in the parkour course
