#!/bin/bash -e

idx=0
for device in $(find /dev/ -type l -regex ".*/google-local-nvme-ssd-.*"); do
	mount_path="/mnt/disks/data-${idx}"
	sudo mkfs.ext4 -F "$device"
	sudo mkdir -p "$mount_path"
	sudo mount "$device" "$mount_path"
	sudo chmod a+w "$mount_path"
	echo "$device $mount_path ext4 discard,defaults,nofail 0 2" | sudo tee -a /etc/fstab
	((idx++))
done
