
# rate-limited-allow-list-admin-frontend

This service is intended to reduce the amount of code service teams need to write, maintain and later remove for managing a gradual onboarding of users to a service or feature, for example as part of a private beta launch. It is responsible for storing the user identifiers in an allow list, storing the parameters for onboarding, and allow the service can check against that list.

In this frontend service the admin users can configure the onboarding parameters, which includes:
- the percentage of new users to be added to the allow list
- a limit on the number of new users
- a limit on the number of new users in a time window
- the status of onboarding which can be either active or paused.

A user who is not an owner of a service can view, but not change, the onboarding parameters.

It is used in combination with [rate-limited-allow-list](https://github.com/hmrc/rate-limited-allow-list), which is responsible for storing the list of allowed identifiers that services can check against. The [readme for rate limited allow list](https://github.com/hmrc/user-allow-list/blob/main/README.md) provides a more complete description of what is and isn't supported, along with integration instructions.

## Accessing the service

Access to the service is via the `admin-frontend-proxy`, using the URL route `/administer-rate-limited-allow-list` after the admin URL the desired environment. You will be required to login using LDAP as this identifies which services you can setup and manage allow lists for.

For security and traceability purposes, any changes to an identifier list are audited including who made those changes.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").