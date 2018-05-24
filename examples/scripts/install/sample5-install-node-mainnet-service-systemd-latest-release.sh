#!/bin/bash
if [[ $EUID -ne 0 ]]; then
   echo "This script requires root!"
   exit 1
fi

snowblossom_home=/var/snowblossom
latest_release=`wget -qO - https://api.github.com/repos/snowblossomcoin/snowblossom/releases`
release_name=`echo "$latest_release" | grep -Po -m 1 '"name": "\K.*?(?=")'`
release_tag=`echo "$latest_release" | grep -Po -m 1 '"tag_name": "\K.*?(?=")'`

echo "Installing snowblossom $release_name $release_tag in $snowblossom_home"

# install openjdk-8-jdk and bazel
echo "deb [arch=amd64] http://storage.googleapis.com/bazel-apt stable jdk1.8" > /etc/apt/sources.list.d/snowblossom-bazel.list
wget -qO - https://bazel.build/bazel-release.pub.gpg | apt-key add -
apt-get update
apt-get -yq install git openjdk-8-jdk bazel

# create and switch to user
useradd --home-dir /var/snowblossom/ --create-home --system snowblossom
su - snowblossom <<EOF

# download source code
mkdir -p "$snowblossom_home/source" && cd "$snowblossom_home/source"
rm -rf snowblossom
git clone -b $release_tag https://github.com/snowblossomcoin/snowblossom.git

# build snowblossom
cd snowblossom
bazel build :all

# copy sample config files
cp --no-clobber --recursive "$snowblossom_home/source/snowblossom/examples/configs" "$snowblossom_home/"
chmod 750 -R "$snowblossom_home/configs"

EOF

# install systemd service
cp "$snowblossom_home/source/snowblossom/examples/systemd/snowblossom-node-mainnet.service" /etc/systemd/system/
systemctl daemon-reload
# startup automatically at boot
systemctl enable snowblossom-node-mainnet.service
# start
systemctl restart snowblossom-node-mainnet.service
journalctl -f -u snowblossom-node-mainnet.service
