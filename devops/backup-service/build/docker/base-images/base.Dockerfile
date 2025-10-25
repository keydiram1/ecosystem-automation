# syntax=docker/dockerfile:1
FROM registry.access.redhat.com/ubi9:latest as base

ARG AWS_CPP_SDK_VERSION="1.10.55"
ARG LIBCURL_VERSION="curl-7_83_1"
ARG LIBUV_VERSION="v1.42.0"
ARG LIBJANSSON_VERSION="v2.14"
ARG LIBZSTD_VERSION="v1.5.5"
ARG OPENSSL_VERSION="openssl-3.0.8"
ARG GO_VERSION=1.22.0
ARG TARGETARCH
WORKDIR /app

RUN dnf -y install make cmake automake libtool zlib-devel git perl openssl-devel && dnf clean all

RUN <<-EOF
  set -e
  curl -Lo /tmp/go.tgz "https://go.dev/dl/go${GO_VERSION}.linux-${TARGETARCH}.tar.gz"
  tar -xzf /tmp/go.tgz -C /usr/local/
  rm /tmp/go.tgz

  git clone https://github.com/openssl/openssl.git
  cd openssl
  git fetch --all --tags
  git checkout tags/"$OPENSSL_VERSION"
  git submodule update --init --recursive
  ./Configure --release -no-pinshared -no-pinshared -no-tests -no-asm
  make
  make install
  cd -
  rm -rf openssl

  git clone --recurse-submodules https://github.com/curl/curl.git
  cd curl
  git fetch --all --tags
  git checkout tags/"$LIBCURL_VERSION"
  autoreconf -i
  ./configure --disable-shared --enable-static --with-pic --with-openssl --disable-debug --disable-curldebug --disable-manual
  make
  make install
  cd -
  rm -rf curl

  git clone --recurse-submodules https://github.com/facebook/zstd
  cd zstd
  git fetch --all --tags
  git checkout tags/"$LIBZSTD_VERSION"
  mkdir -p ./build/cmake/builddir
  cmake -S ./build/cmake -B ./build/cmake/builddir -DZSTD_BUILD_SHARED=OFF -DCMAKE_BUILD_TYPE=Release -DZSTD_BUILD_TESTS=OFF
  make -C build/cmake/builddir
  make -C build/cmake/builddir install
  cd -
  rm -rf zstd

  git clone --recurse-submodules https://github.com/aws/aws-sdk-cpp
  cd aws-sdk-cpp
  git fetch --all --tags
  git checkout tags/"$AWS_CPP_SDK_VERSION"
  cmake -S . -B build \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_ONLY="s3" \
  -DBUILD_SHARED_LIBS=OFF \
  -DENABLE_TESTING=OFF \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DCMAKE_INSTALL_LIBDIR=lib \
  -DENABLE_UNITY_BUILD=ON
  make -C build
  make -C build install
  cd -
  rm -rf aws-sdk-cpp

  git clone https://github.com/libuv/libuv.git
  cd libuv
  git fetch --all --tags
  git checkout tags/"$LIBUV_VERSION"
  sh autogen.sh
  ./configure --with-pic --disable-shared --enable-static --disable-debug
  make
  make install
  cd -
  rm -rf libuv

  git clone https://github.com/akheron/jansson.git
  cd jansson
  git fetch --all --tags
  git checkout tags/"$LIBJANSSON_VERSION"
  autoreconf -i
  ./configure --with-pic --enable-shared=no --enable-static=yes
  make
  make install
  cd -
  rm -rf jansson
EOF
