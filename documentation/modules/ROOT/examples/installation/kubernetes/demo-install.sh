#!/usr/bin/env bash

# cd /media/psf/Home/Workspace/gitlab.com/davinkevin/Podcast-Server/documentation
# asciinema rec demo-install.cast --overwrite -c "bash modules/ROOT/examples/installation/kubernetes/demo-install.sh"

source demo-magic.sh -n

cd "$(mktemp -d)" || exit
DEMO_PROMPT="\[\033[01;32m\]demo-install \[\033[00m\]\$ "

clear

p "# Complete installation with KPT, Kustomize and K3S"

p "# Install k3s"
pe "curl -sfL https://get.k3s.io | sudo sh -"
p "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml"
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
p 'alias kubectl="k3s kubectl"'
alias kubectl="k3s kubectl"
sudo chmod 775 /etc/rancher/k3s/k3s.yaml

p "# Install kustomize"
pe 'curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash'
pe "sudo mv kustomize /usr/local/bin"
sudo chmod +x /usr/local/bin/kustomize

p "# Install kpt"
pe 'sudo curl -qs "https://storage.googleapis.com/kpt-dev/latest/linux_amd64/kpt" -o /usr/local/bin/kpt'
sudo chmod +x /usr/local/bin/kpt
pe "kpt version"

p "# Install Podcast Server"

pe "kubectl create namespace podcast-server"
pe 'kpt pkg get "https://gitlab.com/davinkevin/Podcast-Server.git/distribution/kpt/podcast-server" podcast-server'
pe 'kpt cfg set podcast-server namespace podcast-server'
p 'PODCAST_SERVER_LOCATION=/opt/podcast-server'
export PODCAST_SERVER_LOCATION=/opt/podcast-server
p 'kpt cfg set podcast-server install-location $PODCAST_SERVER_LOCATION'
kpt cfg set podcast-server install-location $PODCAST_SERVER_LOCATION
pe 'sudo mkdir -p $PODCAST_SERVER_LOCATION/files/ $PODCAST_SERVER_LOCATION/database/ $PODCAST_SERVER_LOCATION/database/backup/ $PODCAST_SERVER_LOCATION/files/'
sudo mkdir -p $PODCAST_SERVER_LOCATION/files/ $PODCAST_SERVER_LOCATION/database/ $PODCAST_SERVER_LOCATION/database/backup/ $PODCAST_SERVER_LOCATION/files/
pe 'kustomize build podcast-server | kubectl apply -f -'

pe "until curl -ks --fail https://localhost/actuator/info && echo ; do sleep 1; done"
p "# installation complete 🎉"

sleep 5
