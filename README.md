# KeycloakStuff

Мой плагин для keycloak, с недостающим мне функционалом. На данный момент включает в себя:

* Аунтефикатор `idp-find-existing-broker-user` - Полная копия `idp-detect-existing-broker-user`, за исключение того, что
  вместо ошибки, просто пропускает этап
* Аунтефикатор `idp-set-attribute-from-query` - Позволяет прокинуть аттрибуты пользователя через query параметры при
  первом входе через социальные штуки. Проще говоря, если в урле будет username=aboba, то пользователю автоматом
  подставится это имя, в случае регистрации через например гугл
  `/realms/{realm}/protocol/openid-connect/auth?username=aboba1234`
* Аунтефикатор `block-google-workspace` - Блокирует логины Google Workspace аккаунтов (любой `hd` claim в id_token).
  Пропускает только личные Google-аккаунты (`@gmail.com` и т.п.). Использовать в First Broker Login flow
    * Ключ локализации ошибки: `spliterash.authenticator.google-workspace-blocked-error`
* Валидатор `cooldown-validator` - Позволяет ограничить частоту смены какого либо поля пользователем, при этом
  администратор ограничению не подвергается
    * Ключ локализации: `spliterash.validation.cooldown-validation-error` (можно переопределить
      через Realm -> Localization -> Realm overrides)

      Например: 'Вы сможете обновить {{0}} только через {{1}} секунд'
* Валидатор `http-request-validator` - Позволяет организовать валидацию проперти через api вызов. Осторожно, значение
  настроек передаётся клиенту, мне впадлу это исправлять для своего случая
* Расширение ресурса пользовательских аккаунтов
    * `DELETE` `https://example.com/realms/{realm}/spliterash-account-extension/applications/{client}/access`
      Отозвать доступ к сессии у внутреннего клиента (ПОЧЕМУ ЭТОГО НЕТ В КОРОБКЕ ???)