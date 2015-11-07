/**
* Created by kevin on 07/11/2015 for Podcast Server
*/
import angular from 'angular';

export function RouteConfig({ path, as, controller, reloadOnSearch, resolve}) {
    return Target => {
        if (!Target.$template) throw new TypeError("Template should be defined");
        if (!path) throw new TypeError("Path should be Defined");

        Target.routeConfig = ($routeProvider) => {
            "ngInject";
            $routeProvider.when(path, {
                template: Target.$template,
                hotkeys : Target.$hotKeys,
                controller: controller || Target.name,
                controllerAs : as || 'vm',
                reloadOnSearch : angular.isDefined(reloadOnSearch) ? reloadOnSearch : true,
                resolve : angular.isDefined(resolve) ? resolve : {}
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
export function HotKeys({useDefault, hotKeys}) {
    return Target => {
        Target.$hotKeys = useDefault ? hotKeys.concat(defaultKeys) : hotKeys;
    };
}

export function Component({restrict, scope, as, bindToController, selector}) {
    return Target => {
        if (!Target.$template) throw new TypeError("A Template should be defined with the annotation @View");
        if (!selector) throw new TypeError("A selector should be defined in the current annotation @Component");

        Target.$componentName = snakeCaseToCamelCase(selector);

        Target.component = () => {
            return {
                restrict : restrict || 'E',
                template: Target.$template,
                scope : angular.isDefined(scope) ? scope : true,
                controller : Target.name,
                controllerAs : as || 'vm',
                bindToController : angular.isDefined(bindToController) ? bindToController : true,
                link : Target.link || angular.noop
            };
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