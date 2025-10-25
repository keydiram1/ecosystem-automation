locals {
  istio_index = yamldecode(data.http.istio_index.response_body)
  # base_istio_version = local.istio_index.entries.base.0.version
  # istiod_istio_version = local.istio_index.entries.istiod.0.version
  # gateway_istio_version =local.istio_index.entries.gateway.0.version

  base_istio_version    = "1.25.3"
  istiod_istio_version  = "1.25.3"
  gateway_istio_version = "1.25.3"
}
