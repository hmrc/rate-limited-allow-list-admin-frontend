
# rate-limited-allow-list-admin-frontend

This service is intended to reduce the amount of code service teams need to write, maintain and later remove for managing a gradual rollout of a service or feature to a group of users (for example, as part of a private beta rollout). It is responsible for storing a list of the user's identifiers and the rollout limits, and allow the service can check against that list.

Specifically you can:

- Specify how many new users you want to onboard
- Specify the maximum number of users you want to onboard
- Stop onboarding for your service

These actions can all be performed for multiple allow lists for a single service, by providing a "feature" argument that acts as a name for an allow list.

It is used in combination with [rate-limited-allow-list](https://github.com/hmrc/rate-limited-allow-list), which is responsible for storing the list of allowed identifiers that services can check against. The [readme for rate limited allow list](https://github.com/hmrc/user-allow-list/blob/main/README.md) provides a more complete description of what is and isn't supported, along with integration instructions.

## Accessing the service

Access to the service is via the `admin-frontend-proxy`, using the URL route `/administer-rate-limited-allow-list` after the admin URL the desired environment. You will be required to login using LDAP as this identifies which services you can setup and manage allow lists for.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").