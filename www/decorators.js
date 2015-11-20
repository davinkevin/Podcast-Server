/**
* Created by kevin on 07/11/2015 for Podcast Server
*/

import angular from 'angular';

export function Module({name, inject, modules = []}) {
    return Target => {

        if (angular.isDefined(name) && angular.isDefined(inject))
            throw new TypeError ("Name and Inject can't be define in the same @Module");

        if (!Target.component && !Target.routeConfig && !Target.$serviceName)
            throw new TypeError ("A @Component, @RouteConfig or @Service should be defined first");

        Target.$angularModule = angular.isUndefined(inject) ? angular.module(name, modules) : inject;

        if (Target.component) {
            Target.$angularModule.directive(Target.$componentName, Target.component);
            return;
        }

        if (Target.routeConfig) {
            Target.$angularModule.config(Target.routeConfig);
            return;
        }

        Target.$angularModule.service(Target.$serviceName, Target);
    };
}

export function RouteConfig({ path, as = 'vm', reloadOnSearch = true, resolve = {}}) {
    return Target => {
        if (!Target.$template) throw new TypeError("Template should be defined");
        if (!path) throw new TypeError("Path should be Defined");

        Target.routeConfig = ($routeProvider) => {
            "ngInject";
            $routeProvider.when(path, {
                template: Target.$template,
                hotkeys : Target.$hotKeys,
                controller: Target,
                controllerAs : as,
                reloadOnSearch : reloadOnSearch,
                resolve : resolve
            });
        };
    };
}

let defaultKeys = [
    ['h', 'Goto Home', (event) => {
        event.preventDefault();
        window.location.href = '/items';
    }],
    ['s', 'Goto Search', (event) =>  {
        event.preventDefault();
        window.location.href = '/item/search';
    }],
    ['p', 'Goto Podcast List', (event) =>  {
        event.preventDefault();
        window.location.href = '/podcasts';
    }],
    ['d', 'Goto Download List', (event) =>  {
        event.preventDefault();
        window.location.href = '/download';
    }]
];
export function HotKeys({useDefault = true, hotKeys = []}) {
    return Target =>  {
        Target.$hotKeys = useDefault ? defaultKeys.concat(hotKeys) : hotKeys;
    };
}

export function Component({restrict = 'E', scope = true, as = 'vm', bindToController = true, replace = false, transclude = false, selector}) {
    return Target => {
        if (!Target.$template) throw new TypeError("A Template should be defined with the annotation @View");
        if (!selector) throw new TypeError("A selector should be defined in the current annotation @Component");

        Target.$componentName = snakeCaseToCamelCase(selector);
        Target.component = () => {
            let ddo = {
                restrict : restrict,
                transclude : transclude,
                replace : replace,
                template: Target.$template,
                scope : scope,
                controller : Target,
                controllerAs : as,
                bindToController : bindToController
            };

            Target.link && (ddo.link = Target.link);

            return ddo;
        };
    };
}

export function View({template}) {
    return Target => {
        Target.$template = template;
    };
}

function snakeCaseToCamelCase(string) {
    return string.replace( /-([a-z])/ig, (_,letter) => letter.toUpperCase());
}