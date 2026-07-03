# K8s / Istio 灰度部署

这个目录提供真实集群落地用的 YAML：

- `namespace.yaml`：命名空间
- `demo-order.yaml`：`demo-order-v1` / `demo-order-v2` 两个版本 Deployment 与统一 Service
- `istio-gray-route.yaml`：Istio `DestinationRule` + `VirtualService`

应用：

```bash
kubectl apply -f namespace.yaml
kubectl apply -f demo-order.yaml
kubectl apply -f istio-gray-route.yaml
```

默认流量：

- Header `x-gray: true`：100% 到 `v2`
- 普通流量：90% 到 `v1`，10% 到 `v2`

