# QA checklist — Chatyar 0.3 beta

- [ ] `./gradlew --no-daemon clean assembleDebug` completes without errors.
- [ ] On Android 8/12/15 chat input stays fully above the software keyboard.
- [ ] Chat list shrinks and scrolls to latest messages when keyboard opens.
- [ ] Keyboard Send button submits the message, Stop cancels active generation.
- [ ] Provider editing forms are scrollable with keyboard visible.
- [ ] OpenAI-compatible image service returns b64_json image.
- [ ] Image service returning temporary HTTPS URL downloads image without forwarding API key.
- [ ] Unsupported image API returns a useful error, not a phantom result.
- [ ] New image appears in Recent Images after leaving/reopening screen.
- [ ] Save Image opens document picker and the exported PNG is readable.
- [ ] VLESS and ss:// profiles are stored encrypted; sharing to external VPN app requires user action.
- [ ] VPN detection reflects active Android system transport; manual refresh works.
- [ ] Test Provider checks real API after activation via external VPN client.
- [ ] 403 response is presented as access error, not guaranteed "sanctions" diagnosis.
- [ ] Existing providers, history and secrets survive upgrade (Room schema unchanged).
