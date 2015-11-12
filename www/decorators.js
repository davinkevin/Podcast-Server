/**
* Created by kevin on 07/11/2015 for Podcast Server
*/
import _ from 'lodash';

export function RouteConfig({ path, as = 'vm', controller, reloadOnSearch = true, resolve = {}}) {
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
            return _({
                restrict : restrict,
                transclude : transclude,
                replace : replace,
                template: Target.$template,
                scope : scope,
                controller : Target,
                controllerAs : as,
                bindToController : bindToController,
                link : Target.link
            }).omit(_.isUndefined).value();
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