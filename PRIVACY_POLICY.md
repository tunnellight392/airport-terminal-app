# Privacy Policy for Airport Maps

**Effective date:** 26 September 2026
**App:** Airport Maps (`com.tunnellight.airport_terminal`)

Airport Maps helps you look up US airports and browse their terminals, concourses, airlines and
terminal maps. This policy explains what the app does and does not do with data.

## Summary

**Airport Maps does not collect, store or transmit any personal information.** There are no
accounts, no sign-in, no analytics, no advertising, no tracking identifiers and no crash
reporting. Nothing you type or tap is sent to us, because there is no server of ours for it to be
sent to.

## Information we do not collect

We do not collect, and the app does not ask for:

- Your name, email address, phone number or any other contact details
- Your account or device identifiers (advertising ID, device ID and the like)
- Your location — the app requests no location permission and does not use location services
- Your contacts, photos, files, camera, microphone or calendar
- Analytics, usage statistics or behavioural data

## Permissions the app requests

The app declares only two Android permissions, both classed by Android as *normal* permissions
(they are granted at install and show no runtime prompt):

| Permission | Why it is needed |
| --- | --- |
| `INTERNET` | To download the public airport dataset described below, and to open airport map links in your browser. |
| `ACCESS_NETWORK_STATE` | To check whether a network connection is available before attempting that download. |

## Data stored on your device

Most airport information ships inside the app itself and needs no network access at all. To cover
the full list of US airports, the app also downloads one public dataset the first time it runs and
saves it in the app's private storage so it does not have to download it again. That cached file is
refreshed at most once every 30 days.

This cache contains only public airport reference data — codes, names, cities and states. It
contains nothing about you, and it is removed when you uninstall the app or clear the app's data.

Your search terms are used only to filter this local list while you type. They are never stored and
never leave your device.

Android's own Backup and Restore feature is enabled for this app, so Android may include the app's
stored files in the device backup associated with your Google Account. That backup is handled by
Google under [Google's Privacy Policy](https://policies.google.com/privacy), not by us, and the
files involved contain only the public airport data described above.

## Network connections the app makes

The app contacts only the following, and sends no information about you to either:

1. **`raw.githubusercontent.com`** — over HTTPS, to download the public
   [mwgg/Airports](https://github.com/mwgg/Airports) dataset, pinned to a fixed version. This is a
   plain file download; the request carries no data about you beyond what any HTTPS request
   necessarily reveals to the host serving it (such as your IP address, which GitHub handles under
   its own privacy policy).
2. **Links you choose to open.** Where an airport has an official terminal map, or where the app
   offers to search for one, tapping that link hands it to your browser or another app you have
   installed. From that point the site you visit — an airport's own website, or Google Search — is
   governed by that site's privacy policy, not this one. No link is opened without you tapping it.

If either connection fails, the app keeps working with the airport data bundled inside it.

## Children's privacy

The app is a general-purpose travel reference tool. It collects no personal information from anyone,
including children under 13.

## Third-party services

The app contains no third-party analytics, advertising, attribution or social SDKs. The only
third-party code it uses is standard Android user-interface and utility libraries from Google and
JetBrains, which run entirely on your device and send no data anywhere.

## Your rights

Because we hold no data about you, there is nothing for us to access, export, correct or delete on
your behalf. To remove everything the app has stored locally, clear the app's data or uninstall it.

## Changes to this policy

If the app's behaviour changes in a way that affects this policy, we will update this document and
revise the effective date above. Material changes will be noted in the app's release notes.

## Contact

Questions about this policy can be sent to: `tunnellightt392@gmail.com`
