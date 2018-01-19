/**
 * Created by kevin on 16/10/2016.
 */

/* Entity */
export interface Cover {
  id: string;
  url: string;
  width: number;
  height: number;
}

export interface Item {
  id: string;
  cover: Cover;
  title: string;
  url: string;
  mimeType: string;
  status: Status | string;
  creationDate: string;
  isDownloaded: boolean;
  proxyURL: string;
  podcastId: string;
}

export interface Podcast {
  id: string;
  title: string;
  type: string;
  lastUpdate: string;
  cover: Cover;
}

export interface Tag {
  id: string;
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

export interface SearchItemPageRequest {
  q?: string;
  page?: number;
  size?: number;
  status?: Status[];
  tags?: Tag[];
  sort?: Sort[];
}
