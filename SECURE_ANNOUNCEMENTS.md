# Signed remote announcements

HubSwap 1.0.8 accepts `announcement.json` only when the exact file bytes have a valid Ed25519 signature in `announcement.json.sig`.

## Publish an announcement

1. Edit `announcement.json`.
2. Keep the private key outside the repository.
3. From the repository root run:

```bash
java tools/SignAnnouncement.java announcement.json /path/to/hubswap-announcement-private.pem
```

4. Commit/publish both `announcement.json` and `announcement.json.sig` together.

Never commit or share the private key. If it is lost, a new mod build with a new embedded public key is required. If it is exposed, rotate it immediately and release a new mod build.
