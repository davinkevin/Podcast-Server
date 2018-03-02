/**
 * Created by kevin on 16/10/2016.
 */

export type uuid = string;

/* Entity */
export interface Cover {
  id: uuid;
  url: string;
  width: number;
  height: number;
}

export interface Item {
  id: uuid;
  cover: Cover;
  description?: string;
  pubDate?: string|Date;
  downloadDate?: string|Date;
  creationDate: string|Date;
  title: string;
  url: string;
  mimeType: string;
  status: Status | string;
  isDownloaded: boolean;
  proxyURL: string;
  podcastId: string;
  length?: number;
  fileName?: string;
  progression?: number;
}

export interface Podcast {
  id: uuid;
  title: string;
  type: string;
  lastUpdate: string;
  cover: Cover;
  creationDate?: Date | string;
  description?: string;
  hasToBeDeleted?: boolean;
  tags?: Tag[];
  url?: string;
}

export interface Tag {
  id: uuid;
  name: string;
}

export interface Page<T> {
  content: Array<T>;
  first: boolean;
  last: boolean;
  totalPages: number;
  totalElements: number;
  numberOfElements: number;
  size: number;
  number: number;
  sort: Array<Sort>;
}

export interface Sort {
  direction: Direction | string;
  property: string;
  ignoreCase?: boolean;
  nullHandling?: string,
  descending?: boolean;
  ascending?: boolean;
}

export enum Direction {
  ASC = <any>'ASC',
  DESC = <any>'DESC'
}

export enum Status {
  NOT_DOWNLOADED = <any>'NOT_DOWNLOADED',
  DELETED = <any>'DELETED',
  STARTED = <any>'STARTED',
  FINISH = <any>'FINISH',
  STOPPED = <any>'STOPPED',
  PAUSED = <any>'PAUSED',
  FAILED = <any>'FAILED'
}

export interface Pageable {
  page?: number;
  size?: number;
  sort?: Sort[];
}

export interface SearchItemPageRequest extends Pageable {
  q?: string;
  status?: Status[];
  tags?: Tag[];
}
