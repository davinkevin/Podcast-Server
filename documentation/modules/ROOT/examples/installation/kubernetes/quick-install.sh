#!/usr/bin/env bash

function install_deps() {
  pushd . && \
  cd "$(mktemp -d)" && \
  apt install -y sudo asciinema curl git && \

  curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash && \
  mv kustomize /usr/local/bin && \

  curl -qs "https://storage.googleapis.com/kpt-dev/latest/linux_amd64/kpt" -o /usr/local/bin/kpt && \

  curl -qsL https://github.com/derailed/k9s/releases/download/v0.21.2/k9s_Linux_x86_64.tar.gz -o k9s.tar.gz && \
  tar xvf k9s.tar.gz && \
  mv k9s /usr/local/bin && \
  chmod +x /usr/local/bin/kpt /usr/local/bin/kustomize /usr/local/bin/k9s && \

  curl -sfL https://get.k3s.io | sh - && \
  popd || exit
}

export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
alias kubectl="k3s kubectl"
kubectl create namespace podcast-server

kpt pkg get "https://gitlab.com/davinkevin/Podcast-Server.git/distribution/kpt/podcast-server" podcast-server
kpt cfg set podcast-server namespace podcast-server
PODCAST_SERVER_LOCATION=/opt/podcast-server
kpt cfg set podcast-server install-location $PODCAST_SERVER_LOCATION
mkdir -p $PODCAST_SERVER_LOCATION/files/ $PODCAST_SERVER_LOCATION/database/ $PODCAST_SERVER_LOCATION/database/backup/ $PODCAST_SERVER_LOCATION/files/
kustomize build podcast-server | kubectl apply -f -

curl -ks https://localhost/actuator/info
